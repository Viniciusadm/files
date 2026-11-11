import type { FileItem, Folder, RelayService, Message, UploadSource } from '@relay/core';
import { Store } from './store.ts';

export type TransferStatus = 'queued' | 'running' | 'done' | 'error' | 'cancelled';

export interface Transfer {
  id: number;
  direction: 'upload' | 'download';
  name: string;
  folderName: string | null;
  done: number;
  total: number;
  status: TransferStatus;
  error: string | null;
  startedAt: number | null;
}

interface Job {
  run: (signal: AbortSignal, report: (done: number, total: number) => void) => Promise<void>;
  controller: AbortController | null;
}

const LIMITS = { upload: 2, download: 3 } as const;

export function blobSource(file: File): UploadSource {
  return {
    name: file.name,
    size: file.size,
    type: file.type,
    read: async (offset, length) => new Uint8Array(await file.slice(offset, offset + length).arrayBuffer()),
  };
}

export function saveBlob(blob: Blob, name: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = name;
  anchor.rel = 'noopener';
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

export async function decryptToBlob(
  service: RelayService,
  file: FileItem,
  isSecure: boolean,
  options: { signal?: AbortSignal; onProgress?: (done: number, total: number) => void } = {},
): Promise<Blob> {
  const parts: BlobPart[] = [];
  await service.download(file, isSecure, (plain) => {
    parts.push(plain as Uint8Array<ArrayBuffer>);
  }, {
    signal: options.signal,
    onProgress: options.onProgress ? ({ done, total }) => options.onProgress?.(done, total) : undefined,
  });
  return new Blob(parts, { type: file.mimeType || 'application/octet-stream' });
}

export class Transfers {
  readonly store = new Store<{ items: Transfer[] }>({ items: [] });
  private jobs = new Map<number, Job>();
  private nextId = 1;
  private lastEmit = 0;
  private pendingEmit: ReturnType<typeof setTimeout> | null = null;

  constructor(private readonly service: RelayService, private readonly onUploaded: (message: Message) => void) {}

  upload(folder: Folder, files: File[]): void {
    for (const file of files) {
      this.add('upload', file.name, folder.name, file.size, async (signal, report) => {
        const message = await this.service.sendFile(folder, blobSource(file), {
          signal,
          onProgress: ({ done, total }) => report(done, total),
        });
        this.onUploaded(message);
      });
    }
  }

  download(file: FileItem, isSecure: boolean): void {
    const name = file.name ?? `arquivo-${file.id}`;
    this.add('download', name, null, file.size, async (signal, report) => {
      const blob = await decryptToBlob(this.service, file, isSecure, { signal, onProgress: report });
      saveBlob(blob, name);
    });
  }

  cancel(id: number): void {
    const job = this.jobs.get(id);
    job?.controller?.abort(new DOMException('Cancelado', 'AbortError'));
    this.update(id, { status: 'cancelled', error: null }, true);
    this.pump();
  }

  retry(id: number): void {
    this.update(id, { status: 'queued', error: null, done: 0 }, true);
    this.pump();
  }

  clearFinished(): void {
    const keep = this.store.get().items.filter((item) => item.status === 'queued' || item.status === 'running');
    for (const id of this.jobs.keys()) {
      if (!keep.some((item) => item.id === id)) this.jobs.delete(id);
    }
    this.store.set({ items: keep });
  }

  hasActive(): boolean {
    return this.store.get().items.some((item) => item.status === 'queued' || item.status === 'running');
  }

  private add(direction: Transfer['direction'], name: string, folderName: string | null, total: number, run: Job['run']): void {
    const id = this.nextId++;
    this.jobs.set(id, { run, controller: null });
    this.store.set((state) => ({
      items: [...state.items, { id, direction, name, folderName, done: 0, total, status: 'queued', error: null, startedAt: null }],
    }));
    this.pump();
  }

  private pump(): void {
    for (const direction of ['upload', 'download'] as const) {
      const items = this.store.get().items.filter((item) => item.direction === direction);
      let running = items.filter((item) => item.status === 'running').length;
      for (const item of items) {
        if (running >= LIMITS[direction]) break;
        if (item.status !== 'queued') continue;
        running++;
        void this.execute(item.id);
      }
    }
  }

  private async execute(id: number): Promise<void> {
    const job = this.jobs.get(id);
    if (!job) return;
    const controller = new AbortController();
    job.controller = controller;
    this.update(id, { status: 'running', startedAt: Date.now() }, true);
    try {
      await job.run(controller.signal, (done, total) => this.update(id, { done, total }));
      this.update(id, { status: 'done', done: this.find(id)?.total ?? 0 }, true);
    } catch (error) {
      if (!controller.signal.aborted) {
        this.update(id, { status: 'error', error: error instanceof Error ? error.message : String(error) }, true);
      }
    } finally {
      job.controller = null;
      this.pump();
    }
  }

  private find(id: number): Transfer | undefined {
    return this.store.get().items.find((item) => item.id === id);
  }

  private update(id: number, patch: Partial<Transfer>, immediate = false): void {
    const items = this.store.get().items;
    const index = items.findIndex((item) => item.id === id);
    if (index < 0) return;
    const current = items[index]!;
    if (current.status === 'cancelled' && patch.status !== 'queued') return;
    items[index] = { ...current, ...patch };
    const now = Date.now();
    if (immediate || now - this.lastEmit > 150) {
      this.lastEmit = now;
      if (this.pendingEmit) clearTimeout(this.pendingEmit);
      this.pendingEmit = null;
      this.store.set({ items: [...items] });
    } else if (!this.pendingEmit) {
      this.pendingEmit = setTimeout(() => {
        this.pendingEmit = null;
        this.lastEmit = Date.now();
        this.store.set({ items: [...this.store.get().items] });
      }, 150);
    }
  }
}
