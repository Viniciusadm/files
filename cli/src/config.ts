import { chmod, mkdir, readFile, rename, writeFile } from 'node:fs/promises';
import { homedir } from 'node:os';
import { dirname, join } from 'node:path';

export interface StoredConfig {
  server?: string;
  token?: string;
  key?: string;
  defaultFolder?: string;
  pulled?: Record<string, string[]>;
}

export const DEFAULT_FOLDER_NAME = 'Transferências';

export function configDir(): string {
  if (process.env.FLES_CONFIG_DIR) return process.env.FLES_CONFIG_DIR;
  const base = process.env.XDG_CONFIG_HOME || (process.platform === 'win32' ? process.env.APPDATA : null) || join(homedir(), '.config');
  return join(base as string, 'fles');
}

function configPath(): string {
  return join(configDir(), 'config.json');
}

export async function loadConfig(): Promise<StoredConfig> {
  try {
    return JSON.parse(await readFile(configPath(), 'utf8')) as StoredConfig;
  } catch {
    return {};
  }
}

export async function saveConfig(config: StoredConfig): Promise<void> {
  const path = configPath();
  await mkdir(dirname(path), { recursive: true, mode: 0o700 });
  const temporary = `${path}.tmp`;
  await writeFile(temporary, `${JSON.stringify(config, null, 2)}\n`, { mode: 0o600 });
  await chmod(temporary, 0o600).catch(() => undefined);
  await rename(temporary, path);
}

export async function updateConfig(change: (config: StoredConfig) => void): Promise<StoredConfig> {
  const config = await loadConfig();
  change(config);
  await saveConfig(config);
  return config;
}

export function effective(config: StoredConfig) {
  return {
    server: process.env.FLES_SERVER || config.server || null,
    token: process.env.FLES_TOKEN || config.token || null,
    key: process.env.FLES_KEY || config.key || null,
    pin: process.env.FLES_PIN || null,
    defaultFolder: process.env.FLES_FOLDER || config.defaultFolder || DEFAULT_FOLDER_NAME,
  };
}
