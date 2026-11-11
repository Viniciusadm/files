export type Credentials =
  | { kind: 'bearer'; token: string }
  | { kind: 'apiToken'; token: string };

export interface ApiClientOptions {
  baseUrl: string;
  credentials?: Credentials | null;
  fetch?: typeof fetch;
  maxRetries?: number;
  onUnauthorized?: () => void;
}

export interface RequestOptions {
  query?: Record<string, string | number | null | undefined>;
  json?: unknown;
  form?: FormData;
  signal?: AbortSignal;
}

interface ResponseEnvelope<T> {
  success?: boolean;
  message?: string;
  body?: T;
  error?: unknown;
}

export class ApiError extends Error {
  constructor(message: string, readonly status: number, readonly details?: unknown) {
    super(message);
  }

  get isNetwork(): boolean {
    return this.status === 0;
  }
}

const RETRYABLE = new Set([429, 502, 503, 504]);

export function normalizeBaseUrl(value: string): string {
  let url = value.trim();
  if (!/^https?:\/\//i.test(url)) url = `https://${url}`;
  url = url.replace(/\/+$/, '');
  if (!/\/api$/.test(url)) url = `${url}/api`;
  return `${url}/`;
}

function sleep(ms: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) return reject(signal.reason);
    const timer = setTimeout(resolve, ms);
    signal?.addEventListener('abort', () => {
      clearTimeout(timer);
      reject(signal.reason);
    }, { once: true });
  });
}

function retryDelay(response: Response | null, attempt: number): number {
  const header = response?.headers.get('Retry-After');
  const seconds = header ? Number(header) : NaN;
  if (Number.isFinite(seconds)) return Math.min(seconds * 1000, 30_000);
  const base = Math.min(500 * 2 ** attempt, 15_000);
  return base * (0.5 + Math.random() * 0.5);
}

function firstValidationMessage(error: unknown): string | null {
  if (typeof error === 'string') return error;
  if (error && typeof error === 'object') {
    for (const value of Object.values(error)) {
      const message = firstValidationMessage(Array.isArray(value) ? value[0] : value);
      if (message) return message;
    }
  }
  return null;
}

export class ApiClient {
  readonly baseUrl: string;
  credentials: Credentials | null;
  private readonly fetchImpl: typeof fetch;
  private readonly maxRetries: number;
  private readonly onUnauthorized?: () => void;

  constructor(options: ApiClientOptions) {
    this.baseUrl = normalizeBaseUrl(options.baseUrl);
    this.credentials = options.credentials ?? null;
    this.fetchImpl = options.fetch ?? ((input, init) => globalThis.fetch(input, init));
    this.maxRetries = options.maxRetries ?? 4;
    this.onUnauthorized = options.onUnauthorized;
  }

  url(path: string, query?: RequestOptions['query']): string {
    const relative = path.replace(/^\/?(api\/)?/, '');
    const url = new URL(relative, this.baseUrl);
    for (const [name, value] of Object.entries(query ?? {})) {
      if (value !== null && value !== undefined && value !== '') url.searchParams.set(name, String(value));
    }
    return url.toString();
  }

  headers(extra: Record<string, string> = {}): Record<string, string> {
    const headers: Record<string, string> = { Accept: 'application/json', ...extra };
    if (this.credentials?.kind === 'bearer') headers.Authorization = `Bearer ${this.credentials.token}`;
    if (this.credentials?.kind === 'apiToken') headers['X-API-Token'] = this.credentials.token;
    return headers;
  }

  async raw(method: string, path: string, options: RequestOptions = {}): Promise<Response> {
    const extra: Record<string, string> = {};
    let body: BodyInit | undefined;
    if (options.json !== undefined) {
      extra['Content-Type'] = 'application/json';
      body = JSON.stringify(options.json);
    } else if (options.form) {
      body = options.form;
    }

    for (let attempt = 0; ; attempt++) {
      let response: Response | null = null;
      try {
        response = await this.fetchImpl(this.url(path, options.query), {
          method,
          headers: this.headers(extra),
          body,
          signal: options.signal,
        });
      } catch (error) {
        if (options.signal?.aborted) throw error;
        if (attempt >= this.maxRetries) {
          throw new ApiError('Não foi possível conectar ao servidor. Verifique sua conexão.', 0, error);
        }
      }
      if (response && !RETRYABLE.has(response.status)) return response;
      if (attempt >= this.maxRetries && response) return response;
      await sleep(retryDelay(response, attempt), options.signal);
    }
  }

  async request<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
    const response = await this.raw(method, path, options);
    let payload: ResponseEnvelope<T> | null = null;
    try {
      payload = (await response.json()) as ResponseEnvelope<T>;
    } catch {
      payload = null;
    }
    if (!response.ok || payload?.success === false) {
      if (response.status === 401) this.onUnauthorized?.();
      const message = firstValidationMessage(payload?.error) ?? payload?.message ?? `Erro ${response.status} no servidor.`;
      throw new ApiError(message, response.status, payload?.error);
    }
    return payload?.body as T;
  }

  get<T>(path: string, query?: RequestOptions['query'], signal?: AbortSignal): Promise<T> {
    return this.request<T>('GET', path, { query, signal });
  }

  post<T>(path: string, json?: unknown, signal?: AbortSignal): Promise<T> {
    return this.request<T>('POST', path, { json: json ?? {}, signal });
  }

  patch<T>(path: string, json?: unknown): Promise<T> {
    return this.request<T>('PATCH', path, { json: json ?? {} });
  }

  delete<T>(path: string): Promise<T> {
    return this.request<T>('DELETE', path);
  }
}
