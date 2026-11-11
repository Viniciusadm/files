import { ApiError, type ApiClient } from './api.ts';
import { base64ToBytes, utf8 } from './base64.ts';
import { classifyFile, extensionOf, guessMimeType, normalizeKind, type FileKind } from './fileKind.ts';
import { GCM_OVERHEAD, GcmStreamDecryptor, GcmStreamEncryptor } from './gcmStream.ts';
import { RelayKey, KeyError, decryptOrNull } from './key.ts';

export const UPLOAD_CHUNK_SIZE = 2 * 1024 * 1024;
export const MAX_FILE_BYTES = 1024 * 1024 * 1024 - GCM_OVERHEAD;
const READ_SIZE = 1024 * 1024;
const CHUNK_ATTEMPTS = 6;

interface RemoteChannel {
  id: string;
  name: string | null;
  parentId: string | null;
  icon: string | null;
  color: string | null;
  isSecure: boolean | null;
  lastMessage: string | null;
  updatedAt: string | null;
}

interface RemoteAttachment {
  id: string;
  messageId: string;
  fileName: string | null;
  mimeType: string | null;
  size: number | string | null;
  url: string | null;
  fileKind: string | null;
  extension: string | null;
  createdAt: string | null;
}

interface RemoteListItem {
  id: string;
  text: string | null;
  isChecked: boolean | number | null;
  position: number | null;
}

export interface RemoteMessage {
  id: string;
  folderId: string;
  folderIsSecure?: boolean;
  type: string | null;
  content: string | null;
  attachments: RemoteAttachment[] | null;
  listItems: RemoteListItem[] | null;
  isFavorite: boolean | number | null;
  createdAt: string | null;
  updatedAt: string | null;
  deletedAt: string | null;
}

interface SyncPage {
  messages: RemoteMessage[];
  nextCursor: string | null;
  syncedAt: string;
}

interface UploadSession {
  uploadId: string;
  chunkSize: number;
  totalChunks: number;
  nextChunkIndex: number;
  receivedBytes: number;
  size: number;
  status: string;
}

interface SecureMeta {
  configured: boolean;
  keySalt: string | null;
  keyIterations: number | null;
  keyEnvelope: string | null;
}

export interface Folder {
  id: string;
  name: string | null;
  parentId: string | null;
  icon: string;
  color: string;
  isSecure: boolean;
  updatedAt: string | null;
}

export interface FileItem {
  id: string;
  messageId: string;
  folderId: string;
  name: string | null;
  mimeType: string;
  size: number;
  kind: FileKind;
  extension: string;
  url: string;
  createdAt: string | null;
  isFavorite: boolean;
}

export interface ListItem {
  id: string;
  text: string | null;
  checked: boolean;
  position: number;
}

export interface Message {
  id: string;
  folderId: string;
  type: string;
  text: string | null;
  attachments: FileItem[];
  listItems: ListItem[];
  isFavorite: boolean;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface UploadSource {
  name: string;
  size: number;
  type?: string | null;
  read(offset: number, length: number): Promise<Uint8Array>;
}

export interface TransferProgress {
  done: number;
  total: number;
}

export interface TransferOptions {
  onProgress?: (progress: TransferProgress) => void;
  signal?: AbortSignal;
}

export interface UploadedFile {
  uploadId: string;
  metadata: Record<string, string>;
}

function bool(value: unknown): boolean {
  return value === true || value === 1 || value === '1' || value === 'true';
}

function num(value: unknown): number {
  const parsed = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
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

export class RelayService {
  private secureKey: RelayKey | null = null;

  constructor(readonly api: ApiClient, readonly key: RelayKey) {}

  get secureUnlocked(): boolean {
    return this.secureKey !== null;
  }

  keyFor(isSecure: boolean): RelayKey | null {
    return isSecure ? this.secureKey : this.key;
  }

  private requireKey(isSecure: boolean): RelayKey {
    const key = this.keyFor(isSecure);
    if (!key) throw new KeyError('Desbloqueie as pastas seguras para continuar.');
    return key;
  }

  private encryptFor(isSecure: boolean, value: string): Promise<string> {
    return this.requireKey(isSecure).encryptText(value, isSecure ? 2 : 1);
  }

  async secureConfigured(): Promise<boolean> {
    const meta = await this.api.get<SecureMeta>('user/secure-folders/meta');
    return meta.configured;
  }

  async unlockSecure(pin: string): Promise<void> {
    if (!/^\d{4}$/.test(pin)) throw new KeyError('Informe o PIN de 4 dígitos.');
    const meta = await this.api.get<SecureMeta>('user/secure-folders/meta');
    if (!meta.configured || !meta.keySalt || !meta.keyIterations || !meta.keyEnvelope) {
      throw new KeyError('As pastas seguras ainda não foram configuradas no celular.');
    }
    const material = await crypto.subtle.importKey('raw', utf8(`${this.key.encoded}:${pin}`), 'PBKDF2', false, ['deriveBits']);
    const bits = await crypto.subtle.deriveBits(
      { name: 'PBKDF2', hash: 'SHA-256', salt: base64ToBytes(meta.keySalt), iterations: meta.keyIterations },
      material,
      256,
    );
    let raw: Uint8Array;
    try {
      raw = await RelayKey.fromRaw(new Uint8Array(bits)).decryptBytes(meta.keyEnvelope);
    } catch {
      throw new KeyError('PIN incorreto.');
    }
    this.secureKey = RelayKey.fromRaw(raw);
  }

  lockSecure(): void {
    this.secureKey = null;
  }

  async folders(): Promise<Folder[]> {
    const channels = await this.api.get<RemoteChannel[]>('channels');
    return Promise.all(channels.map((channel) => this.toFolder(channel)));
  }

  private async toFolder(channel: RemoteChannel): Promise<Folder> {
    const isSecure = bool(channel.isSecure);
    return {
      id: String(channel.id),
      name: await decryptOrNull(this.keyFor(isSecure), channel.name),
      parentId: channel.parentId ? String(channel.parentId) : null,
      icon: channel.icon ?? 'Folder',
      color: channel.color ?? '#3390ec',
      isSecure,
      updatedAt: channel.updatedAt,
    };
  }

  async createFolder(name: string, options: { parentId?: string | null; isSecure?: boolean; color?: string; icon?: string } = {}): Promise<Folder> {
    const isSecure = options.isSecure ?? false;
    const created = await this.api.post<RemoteChannel>('folders', {
      name: await this.encryptFor(isSecure, name.trim()),
      parentId: options.parentId ?? null,
      icon: options.icon ?? 'Folder',
      color: options.color ?? '#3390ec',
      isSecure,
    });
    return this.toFolder(created);
  }

  async renameFolder(folder: Folder, name: string): Promise<void> {
    await this.api.patch(`folders/${folder.id}`, { name: await this.encryptFor(folder.isSecure, name.trim()) });
  }

  async deleteFolder(folderId: string): Promise<void> {
    await this.api.delete(`folders/${folderId}`);
  }

  async sync(since: string | null, onPage?: (messages: RemoteMessage[]) => void, signal?: AbortSignal): Promise<{ messages: RemoteMessage[]; syncedAt: string | null }> {
    const all: RemoteMessage[] = [];
    let cursor: string | null = null;
    let syncedAt: string | null = null;
    do {
      const page: SyncPage = await this.api.get<SyncPage>('messages/sync', { since, cursor, limit: 500 }, signal);
      syncedAt ??= page.syncedAt;
      all.push(...page.messages);
      onPage?.(page.messages);
      cursor = page.nextCursor;
    } while (cursor);
    return { messages: all, syncedAt };
  }

  async decryptMessage(remote: RemoteMessage, isSecure: boolean): Promise<Message> {
    const key = this.keyFor(isSecure);
    const folderId = String(remote.folderId);
    const isFavorite = bool(remote.isFavorite);
    const attachments = await Promise.all((remote.attachments ?? []).map(async (attachment): Promise<FileItem> => {
      const name = await decryptOrNull(key, attachment.fileName);
      const displayName = name ?? '';
      const mimeType = guessMimeType(displayName, attachment.mimeType);
      return {
        id: String(attachment.id),
        messageId: String(remote.id),
        folderId,
        name,
        mimeType,
        size: Math.max(num(attachment.size) - GCM_OVERHEAD, 0),
        kind: normalizeKind(attachment.fileKind, displayName, mimeType),
        extension: (attachment.extension || extensionOf(displayName)).toLowerCase(),
        url: attachment.url ?? `/api/attachments/${attachment.id}/download`,
        createdAt: attachment.createdAt ?? remote.createdAt,
        isFavorite,
      };
    }));
    const listItems = await Promise.all((remote.listItems ?? []).map(async (item): Promise<ListItem> => ({
      id: String(item.id),
      text: await decryptOrNull(key, item.text),
      checked: bool(item.isChecked),
      position: num(item.position),
    })));
    listItems.sort((a, b) => a.position - b.position);
    return {
      id: String(remote.id),
      folderId,
      type: remote.type ?? 'text',
      text: await decryptOrNull(key, remote.content),
      attachments,
      listItems,
      isFavorite,
      createdAt: remote.createdAt,
      updatedAt: remote.updatedAt,
    };
  }

  async sendNote(folder: Folder, text: string): Promise<Message> {
    const remote = await this.api.post<RemoteMessage>('messages', {
      folderId: folder.id,
      content: await this.encryptFor(folder.isSecure, text),
    });
    return this.decryptMessage(remote, folder.isSecure);
  }

  async upload(folder: Folder, source: UploadSource, options: TransferOptions = {}): Promise<UploadedFile> {
    if (source.size > MAX_FILE_BYTES) {
      throw new KeyError(`"${source.name}" passa do limite de 1 GB por arquivo.`);
    }
    const key = this.requireKey(folder.isSecure);
    const fileName = await this.encryptFor(folder.isSecure, source.name);
    const mimeType = guessMimeType(source.name, source.type);
    const extension = extensionOf(source.name);
    const fileKind = classifyFile(source.name, mimeType);
    const encryptedSize = source.size + GCM_OVERHEAD;
    const session = await this.api.post<UploadSession>('uploads', {
      fileName,
      mimeType,
      size: encryptedSize,
      chunkSize: UPLOAD_CHUNK_SIZE,
      extension,
      fileKind,
      compressed: false,
    }, options.signal);

    try {
      const encryptor = await GcmStreamEncryptor.create(key);
      const chunkSize = session.chunkSize;
      let buffer = new Uint8Array(chunkSize);
      let buffered = 0;
      let chunkIndex = 0;
      let sent = 0;
      options.onProgress?.({ done: 0, total: encryptedSize });

      const flush = async (length: number) => {
        await this.sendChunk(session, chunkIndex, buffer.slice(0, length), options.signal);
        chunkIndex++;
        sent += length;
        options.onProgress?.({ done: sent, total: encryptedSize });
      };

      const push = async (data: Uint8Array) => {
        let offset = 0;
        while (offset < data.length) {
          const take = Math.min(chunkSize - buffered, data.length - offset);
          buffer.set(data.subarray(offset, offset + take), buffered);
          buffered += take;
          offset += take;
          if (buffered === chunkSize) {
            await flush(buffered);
            buffer = new Uint8Array(chunkSize);
            buffered = 0;
          }
        }
      };

      for (let offset = 0; offset < source.size; offset += READ_SIZE) {
        options.signal?.throwIfAborted();
        const plain = await source.read(offset, Math.min(READ_SIZE, source.size - offset));
        await push(await encryptor.update(plain));
      }
      await push(await encryptor.final());
      if (buffered > 0) await flush(buffered);

      await this.api.post<UploadSession>(`uploads/${session.uploadId}/complete`, {}, options.signal);
    } catch (error) {
      void this.api.delete(`uploads/${session.uploadId}`).catch(() => undefined);
      throw error;
    }

    return {
      uploadId: session.uploadId,
      metadata: {
        fileName,
        mimeType,
        fileKind,
        extension,
        compressed: 'false',
        size: String(encryptedSize),
      },
    };
  }

  private async sendChunk(session: UploadSession, chunkIndex: number, bytes: Uint8Array<ArrayBuffer>, signal?: AbortSignal): Promise<void> {
    for (let attempt = 1; ; attempt++) {
      const form = new FormData();
      form.append('chunkIndex', String(chunkIndex));
      form.append('totalChunks', String(session.totalChunks));
      form.append('chunkSize', String(session.chunkSize));
      form.append('chunk', new Blob([bytes]), `chunk-${chunkIndex}.bin`);
      try {
        await this.api.request<UploadSession>('POST', `uploads/${session.uploadId}/chunks`, { form, signal });
        return;
      } catch (error) {
        if (signal?.aborted) throw error;
        if (error instanceof ApiError && error.status === 409) {
          const status = await this.api.get<UploadSession>(`uploads/${session.uploadId}`, undefined, signal);
          if (status.nextChunkIndex > chunkIndex) return;
        }
        const retryable = error instanceof ApiError && (error.isNetwork || error.status >= 500);
        if (!retryable || attempt >= CHUNK_ATTEMPTS) throw error;
        await sleep(1000 * attempt, signal);
      }
    }
  }

  async createMessage(folder: Folder, uploads: UploadedFile[], text?: string | null): Promise<Message> {
    const form = new FormData();
    form.append('folderId', folder.id);
    if (text && text.trim()) form.append('content', await this.encryptFor(folder.isSecure, text));
    uploads.forEach((upload, index) => {
      form.append(`uploadIds[${index}]`, upload.uploadId);
      for (const [field, value] of Object.entries(upload.metadata)) {
        form.append(`fileMetadata[${index}][${field}]`, value);
      }
    });
    const remote = await this.api.request<RemoteMessage>('POST', 'messages', { form });
    return this.decryptMessage(remote, folder.isSecure);
  }

  async sendFile(folder: Folder, source: UploadSource, options: TransferOptions & { text?: string | null } = {}): Promise<Message> {
    const uploaded = await this.upload(folder, source, options);
    return this.createMessage(folder, [uploaded], options.text);
  }

  async download(file: FileItem, isSecure: boolean, onData: (plain: Uint8Array) => void | Promise<void>, options: TransferOptions = {}): Promise<void> {
    const key = this.requireKey(isSecure);
    const response = await this.api.raw('GET', file.url, { signal: options.signal });
    if (!response.ok || !response.body) {
      throw new ApiError(response.status === 404 ? 'Arquivo não encontrado no servidor.' : `Falha no download (${response.status}).`, response.status);
    }
    const total = Number(response.headers.get('Content-Length')) || file.size + GCM_OVERHEAD;
    const decryptor = new GcmStreamDecryptor(key);
    const reader = response.body.getReader();
    let done = 0;
    options.onProgress?.({ done, total });
    for (;;) {
      const { value, done: finished } = await reader.read();
      if (finished) break;
      done += value.length;
      const plain = await decryptor.update(value);
      if (plain.length > 0) await onData(plain);
      options.onProgress?.({ done, total });
    }
    const tail = await decryptor.final();
    if (tail.length > 0) await onData(tail);
  }

  async renameFile(file: FileItem, isSecure: boolean, name: string): Promise<void> {
    await this.api.patch(`attachments/${file.id}/rename`, { name: await this.encryptFor(isSecure, name.trim()) });
  }

  async deleteFiles(ids: string[]): Promise<void> {
    if (ids.length === 1) {
      await this.api.delete(`attachments/${ids[0]}`);
      return;
    }
    for (let offset = 0; offset < ids.length; offset += 50) {
      await this.api.post('attachments/bulk-delete', { ids: ids.slice(offset, offset + 50) });
    }
  }

  async moveFiles(ids: string[], target: Folder): Promise<void> {
    for (let offset = 0; offset < ids.length; offset += 50) {
      await this.api.post('attachments/bulk-move', { ids: ids.slice(offset, offset + 50), targetFolderId: target.id });
    }
  }

  async deleteMessage(id: string): Promise<void> {
    await this.api.delete(`messages/${id}`);
  }

  async toggleFavorite(messageId: string): Promise<void> {
    await this.api.patch(`messages/${messageId}/favorite`);
  }

  async toggleFileFavorite(fileId: string): Promise<void> {
    await this.api.patch(`attachments/${fileId}/favorite`);
  }

  async toggleListItem(messageId: string, itemId: string): Promise<void> {
    await this.api.patch(`messages/${messageId}/list-items/${itemId}/toggle`);
  }
}
