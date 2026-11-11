import type { FileItem, RelayService } from '@relay/core';
import { decryptToBlob } from './transfers.ts';

const THUMBNAIL_MAX_BYTES = 12 * 1024 * 1024;
const CACHE_LIMIT = 250;
const CONCURRENCY = 3;

export class MediaCache {
  private urls = new Map<string, string>();
  private inflight = new Map<string, Promise<string>>();
  private queue: (() => void)[] = [];
  private active = 0;

  constructor(private readonly service: RelayService) {}

  canThumbnail(file: FileItem): boolean {
    return file.kind === 'image' && file.size > 0 && file.size <= THUMBNAIL_MAX_BYTES && !/heic|heif/i.test(file.mimeType);
  }

  cached(fileId: string): string | null {
    return this.urls.get(fileId) ?? null;
  }

  load(file: FileItem, isSecure: boolean, options: { priority?: boolean; onProgress?: (done: number, total: number) => void; signal?: AbortSignal } = {}): Promise<string> {
    const existing = this.urls.get(file.id);
    if (existing) {
      this.urls.delete(file.id);
      this.urls.set(file.id, existing);
      return Promise.resolve(existing);
    }
    const running = this.inflight.get(file.id);
    if (running && !options.onProgress) return running;
    const task = new Promise<string>((resolve, reject) => {
      const start = () => {
        this.active++;
        decryptToBlob(this.service, file, isSecure, { signal: options.signal, onProgress: options.onProgress })
          .then((blob) => {
            const url = URL.createObjectURL(blob);
            this.remember(file.id, url);
            resolve(url);
          }, reject)
          .finally(() => {
            this.active--;
            this.inflight.delete(file.id);
            this.queue.shift()?.();
          });
      };
      if (options.priority || this.active < CONCURRENCY) start();
      else this.queue.push(start);
    });
    this.inflight.set(file.id, task);
    return task;
  }

  private remember(id: string, url: string): void {
    this.urls.set(id, url);
    while (this.urls.size > CACHE_LIMIT) {
      const [oldestId, oldestUrl] = this.urls.entries().next().value as [string, string];
      this.urls.delete(oldestId);
      URL.revokeObjectURL(oldestUrl);
    }
  }

  clear(): void {
    for (const url of this.urls.values()) URL.revokeObjectURL(url);
    this.urls.clear();
  }
}
