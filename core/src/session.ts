import { ApiClient } from './api.ts';

export interface LoginResult {
  token: string;
  user: { id: string | null; name: string | null; email: string } | null;
}

export interface CreatedApiToken {
  id: string;
  name: string;
  plainTextToken: string;
}

export interface ApiTokenInfo {
  id: string;
  name: string;
  scopes: string[];
  createdAt: string | null;
  lastUsedAt: string | null;
  revokedAt: string | null;
  status: string;
}

export function login(baseUrl: string, email: string, password: string, fetchImpl?: typeof fetch): Promise<LoginResult> {
  const api = new ApiClient({ baseUrl, fetch: fetchImpl, maxRetries: 1 });
  return api.post<LoginResult>('auth/login', { email: email.trim(), password });
}

export function logout(api: ApiClient): Promise<unknown> {
  return api.post('logout');
}

export function createApiToken(api: ApiClient, name: string): Promise<CreatedApiToken> {
  return api.post<CreatedApiToken>('user/api-tokens', { name: name.slice(0, 120), scopes: ['files'] });
}

export function listApiTokens(api: ApiClient): Promise<ApiTokenInfo[]> {
  return api.get<ApiTokenInfo[]>('user/api-tokens');
}

export function revokeApiToken(api: ApiClient, id: string): Promise<unknown> {
  return api.delete(`user/api-tokens/${id}`);
}
