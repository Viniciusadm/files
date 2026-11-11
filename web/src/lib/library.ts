import type { FileItem, Folder, FlesService, Message, RemoteMessage } from '@fles/core';
import { Store } from './store.ts';

export interface LibraryState {
  status: 'loading' | 'ready' | 'error';
  error: string | null;
  folders: Folder[];
  messages: Message[];
  files: FileItem[];
  secureConfigured: boolean;
  secureUnlocked: boolean;
  lastSyncAt: number | null;
}

const DELTA_INTERVAL_MS = 15_000;
const FULL_INTERVAL_MS = 10 * 60_000;

function sortByDate<T extends { createdAt: string | null; id: string }>(items: T[]): T[] {
  return items.sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? '') || Number(b.id) - Number(a.id));
}

export class Library {
  readonly store = new Store<LibraryState>({
    status: 'loading',
    error: null,
    folders: [],
    messages: [],
    files: [],
    secureConfigured: false,
    secureUnlocked: false,
    lastSyncAt: null,
  });

  private raw = new Map<string, RemoteMessage>();
  private decrypted = new Map<string, Message>();
  private syncedAt: string | null = null;
  private lastFull = 0;
  private running: Promise<void> | null = null;
  private timer: ReturnType<typeof setInterval> | null = null;

  constructor(readonly service: FlesService) {}

  start(): () => void {
    void this.refresh(true);
    this.timer = setInterval(() => {
      if (document.visibilityState === 'visible') void this.refresh();
    }, DELTA_INTERVAL_MS);
    const onVisible = () => {
      if (document.visibilityState === 'visible') void this.refresh();
    };
    document.addEventListener('visibilitychange', onVisible);
    window.addEventListener('focus', onVisible);
    return () => {
      if (this.timer) clearInterval(this.timer);
      document.removeEventListener('visibilitychange', onVisible);
      window.removeEventListener('focus', onVisible);
    };
  }

  refresh(full = false): Promise<void> {
    if (this.running) return this.running;
    this.running = this.run(full || Date.now() - this.lastFull > FULL_INTERVAL_MS).finally(() => {
      this.running = null;
    });
    return this.running;
  }

  private async run(full: boolean): Promise<void> {
    try {
      const [folders, secureConfigured] = await Promise.all([
        this.service.folders(),
        this.store.get().lastSyncAt === null ? this.service.secureConfigured().catch(() => false) : Promise.resolve(this.store.get().secureConfigured),
      ]);
      const secureById = new Map(folders.map((folder) => [folder.id, folder.isSecure]));
      const { messages, syncedAt } = await this.service.sync(full ? null : this.syncedAt);
      if (full) {
        this.raw.clear();
        this.decrypted.clear();
        this.lastFull = Date.now();
      }
      for (const message of messages) {
        const id = String(message.id);
        if (message.deletedAt) {
          this.raw.delete(id);
          this.decrypted.delete(id);
          continue;
        }
        this.raw.set(id, message);
        this.decrypted.set(id, await this.service.decryptMessage(message, secureById.get(String(message.folderId)) ?? Boolean(message.folderIsSecure)));
      }
      this.syncedAt = syncedAt ?? this.syncedAt;
      this.publish(folders, { secureConfigured });
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      this.store.set((state) => ({ status: state.status === 'ready' ? 'ready' : 'error', error: message }));
    }
  }

  private publish(folders = this.store.get().folders, extra: Partial<LibraryState> = {}): void {
    const known = new Set(folders.map((folder) => folder.id));
    const messages = sortByDate([...this.decrypted.values()].filter((message) => known.has(message.folderId)));
    const files = sortByDate(messages.flatMap((message) => message.attachments));
    this.store.set({
      status: 'ready',
      error: null,
      folders,
      messages,
      files,
      secureUnlocked: this.service.secureUnlocked,
      lastSyncAt: Date.now(),
      ...extra,
    });
  }

  async unlockSecure(pin: string): Promise<void> {
    await this.service.unlockSecure(pin);
    await this.redecryptSecure();
  }

  async lockSecure(): Promise<void> {
    this.service.lockSecure();
    await this.redecryptSecure();
  }

  private async redecryptSecure(): Promise<void> {
    const folders = await this.service.folders();
    const secureIds = new Set(folders.filter((folder) => folder.isSecure).map((folder) => folder.id));
    for (const [id, message] of this.raw) {
      if (secureIds.has(String(message.folderId))) {
        this.decrypted.set(id, await this.service.decryptMessage(message, true));
      }
    }
    this.publish(folders);
  }

  removeFilesLocally(ids: string[]): void {
    const wanted = new Set(ids);
    for (const [id, message] of this.decrypted) {
      if (message.attachments.some((file) => wanted.has(file.id))) {
        const attachments = message.attachments.filter((file) => !wanted.has(file.id));
        if (attachments.length === 0 && !message.text) this.decrypted.delete(id);
        else this.decrypted.set(id, { ...message, attachments });
      }
    }
    this.publish();
  }

  removeMessageLocally(id: string): void {
    this.decrypted.delete(id);
    this.publish();
  }

  upsertLocally(message: Message): void {
    this.decrypted.set(message.id, message);
    this.publish();
  }

  async afterMutation(): Promise<void> {
    await this.refresh();
  }
}
