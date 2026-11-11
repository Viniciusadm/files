import type { FileItem, RelayService, UploadSource } from '@relay/core';
import { open, readdir, rename, rm, stat } from 'node:fs/promises';
import { basename, extname, join } from 'node:path';
import { progressReporter } from './output.ts';

export async function fileSource(path: string): Promise<UploadSource & { close: () => Promise<void> }> {
  const handle = await open(path, 'r');
  const info = await handle.stat();
  return {
    name: basename(path),
    size: info.size,
    async read(offset, length) {
      const buffer = Buffer.allocUnsafe(length);
      let read = 0;
      while (read < length) {
        const { bytesRead } = await handle.read(buffer, read, length - read, offset + read);
        if (bytesRead === 0) throw new Error(`O arquivo ${path} mudou durante o envio.`);
        read += bytesRead;
      }
      return buffer;
    },
    close: () => handle.close(),
  };
}

export async function expandPaths(paths: string[], recursive: boolean): Promise<string[]> {
  const result: string[] = [];
  for (const path of paths) {
    const info = await stat(path).catch(() => null);
    if (!info) throw new Error(`Caminho não encontrado: ${path}`);
    if (info.isFile()) {
      result.push(path);
    } else if (info.isDirectory()) {
      if (!recursive) throw new Error(`"${path}" é uma pasta. Use -r para enviar o conteúdo dela.`);
      const entries = await readdir(path, { withFileTypes: true });
      const children = entries.filter((entry) => !entry.name.startsWith('.')).map((entry) => join(path, entry.name));
      result.push(...(await expandPaths(children, true)));
    }
  }
  return result;
}

async function exists(path: string): Promise<boolean> {
  return stat(path).then(() => true, () => false);
}

export function safeFileName(name: string | null, file: FileItem): string {
  const cleaned = (name ?? '').replace(/[/\\\0]/g, '_').replace(/^\.+/, '').trim();
  return cleaned || `arquivo-${file.id}${file.extension ? `.${file.extension}` : ''}`;
}

export async function availablePath(directory: string, name: string): Promise<string> {
  let candidate = join(directory, name);
  const extension = extname(name);
  const stem = name.slice(0, name.length - extension.length);
  for (let counter = 1; await exists(candidate); counter++) {
    candidate = join(directory, `${stem} (${counter})${extension}`);
  }
  return candidate;
}

export async function downloadTo(service: RelayService, file: FileItem, isSecure: boolean, directory: string, quiet = false): Promise<string> {
  const target = await availablePath(directory, safeFileName(file.name, file));
  const partial = `${target}.part`;
  const handle = await open(partial, 'w', 0o644);
  const progress = quiet ? null : progressReporter(basename(target));
  try {
    await service.download(file, isSecure, async (plain) => {
      await handle.write(plain);
    }, { onProgress: progress?.update });
    await handle.close();
    await rename(partial, target);
    progress?.done();
    return target;
  } catch (error) {
    await handle.close().catch(() => undefined);
    await rm(partial, { force: true });
    throw error;
  }
}
