import { ApiClient, ApiError, FlesKey, FlesService, type FileItem, type Folder, type Message } from '@fles/core';
import { effective, loadConfig, type StoredConfig } from './config.ts';
import { CliError } from './output.ts';

export interface Library {
  folders: Folder[];
  messages: Message[];
  files: FileItem[];
}

export class Context {
  private library?: Promise<Library>;

  private constructor(readonly config: StoredConfig, readonly service: FlesService, readonly defaultFolderName: string) {}

  static async open(options: { needsKey?: boolean } = {}): Promise<Context> {
    const config = await loadConfig();
    const settings = effective(config);
    if (!settings.server || !settings.token) {
      throw new CliError('Você ainda não entrou. Rode: fles login');
    }
    if (!settings.key && options.needsKey !== false) {
      throw new CliError('Chave de criptografia não configurada. Rode: fles pair (ou fles key set <chave>)');
    }
    const api = new ApiClient({ baseUrl: settings.server, credentials: { kind: 'apiToken', token: settings.token } });
    const key = settings.key ? FlesKey.fromBase64(settings.key) : FlesKey.generate();
    const service = new FlesService(api, key);
    if (settings.pin) await service.unlockSecure(settings.pin);
    return new Context(config, service, settings.defaultFolder);
  }

  load(): Promise<Library> {
    this.library ??= (async () => {
      const folders = await this.service.folders();
      const secureById = new Map(folders.map((folder) => [folder.id, folder.isSecure]));
      const { messages: remote } = await this.service.sync(null);
      const messages = await Promise.all(
        remote.filter((message) => !message.deletedAt).map((message) => this.service.decryptMessage(message, secureById.get(String(message.folderId)) ?? Boolean(message.folderIsSecure))),
      );
      const files = messages.flatMap((message) => message.attachments).sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? '') || Number(b.id) - Number(a.id));
      return { folders, messages, files };
    })();
    return this.library;
  }

  invalidate(): void {
    this.library = undefined;
  }

  folderPath(folder: Folder, folders: Folder[]): string {
    const byId = new Map(folders.map((item) => [item.id, item]));
    const parts: string[] = [];
    let current: Folder | undefined = folder;
    const seen = new Set<string>();
    while (current && !seen.has(current.id)) {
      seen.add(current.id);
      parts.unshift(current.name ?? '🔒');
      current = current.parentId ? byId.get(current.parentId) : undefined;
    }
    return parts.join('/');
  }

  async findFolder(reference: string): Promise<Folder | null> {
    const { folders } = await this.load();
    const byId = folders.find((folder) => folder.id === reference.replace(/^#/, ''));
    if (byId) return byId;
    const wanted = normalize(reference);
    const byPath = folders.filter((folder) => normalize(this.folderPath(folder, folders)) === wanted);
    if (byPath.length > 0) return byPath[0]!;
    const byName = folders.filter((folder) => folder.name !== null && normalize(folder.name) === wanted);
    if (byName.length > 1) {
      throw new CliError(`Há ${byName.length} pastas chamadas "${reference}". Use o caminho completo (ex.: Pai/Filha) ou o id (#${byName[0]!.id}).`);
    }
    return byName[0] ?? null;
  }

  async requireFolder(reference: string): Promise<Folder> {
    const folder = await this.findFolder(reference);
    if (!folder) throw new CliError(`Pasta "${reference}" não encontrada. Veja as pastas com: fles folders`);
    if (folder.name === null) throw new CliError(`A pasta "${reference}" é segura. Defina FLES_PIN para acessá-la.`);
    return folder;
  }

  async targetFolder(reference: string | undefined): Promise<Folder> {
    if (reference) return this.requireFolder(reference);
    const existing = await this.findFolder(this.defaultFolderName);
    if (existing) return existing;
    const created = await this.service.createFolder(this.defaultFolderName, { icon: 'Inbox', color: '#3390ec' });
    this.invalidate();
    return created;
  }

  async resolveFiles(references: string[], folderReference?: string): Promise<FileItem[]> {
    const { files } = await this.load();
    const folder = folderReference ? await this.requireFolder(folderReference) : null;
    const scope = folder ? files.filter((file) => file.folderId === folder.id) : files;
    return references.map((reference) => {
      const byId = scope.find((file) => file.id === reference.replace(/^#/, ''));
      if (byId) return byId;
      const wanted = normalize(reference);
      const matches = scope.filter((file) => file.name !== null && normalize(file.name) === wanted);
      if (matches.length === 0) throw new CliError(`Arquivo "${reference}" não encontrado.`);
      if (matches.length > 1) {
        const ids = matches.slice(0, 5).map((file) => `#${file.id}`).join(', ');
        throw new CliError(`Há ${matches.length} arquivos chamados "${reference}" (${ids}). Use o id ou --folder.`);
      }
      return matches[0]!;
    });
  }

  isSecure(folderId: string, folders: Folder[]): boolean {
    return folders.find((folder) => folder.id === folderId)?.isSecure ?? false;
  }
}

export function normalize(value: string): string {
  return value.normalize('NFD').replace(/[̀-ͯ]/g, '').trim().toLowerCase().replace(/\/+$/, '');
}

export function describeError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401) return 'Sessão inválida ou token revogado. Rode: fles login';
    return error.message;
  }
  if (error instanceof Error) return error.message;
  return String(error);
}
