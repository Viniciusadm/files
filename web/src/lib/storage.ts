export interface StoredSession {
  server: string;
  token: string;
  email: string | null;
  key: string | null;
}

const SESSION_KEY = 'fles.session';
const PREFERENCES_KEY = 'fles.preferences';

export interface Preferences {
  view: 'grid' | 'list';
  lastFolderId: string | null;
}

export const DEFAULT_SERVER: string = (import.meta.env.VITE_API_URL as string | undefined) || `${location.origin}/api`;

function read<T>(name: string): T | null {
  try {
    const raw = localStorage.getItem(name);
    return raw ? (JSON.parse(raw) as T) : null;
  } catch {
    return null;
  }
}

function write(name: string, value: unknown): void {
  try {
    if (value === null) localStorage.removeItem(name);
    else localStorage.setItem(name, JSON.stringify(value));
  } catch {
    return;
  }
}

export function loadSession(): StoredSession | null {
  return read<StoredSession>(SESSION_KEY);
}

export function saveSession(session: StoredSession | null): void {
  write(SESSION_KEY, session);
}

export function loadPreferences(): Preferences {
  return { view: 'grid', lastFolderId: null, ...read<Partial<Preferences>>(PREFERENCES_KEY) };
}

export function savePreferences(change: Partial<Preferences>): void {
  write(PREFERENCES_KEY, { ...loadPreferences(), ...change });
}
