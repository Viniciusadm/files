import {
  ApiClient,
  FlesKey,
  PairingRequest,
  createApiToken,
  formatBytes,
  login,
  logout,
  normalizeBaseUrl,
  type FileItem,
} from '@fles/core';
import { Command, Option } from 'commander';
import { mkdir } from 'node:fs/promises';
import { hostname } from 'node:os';
import { resolve } from 'node:path';
import { effective, loadConfig, updateConfig } from './config.ts';
import { Context, describeError } from './context.ts';
import { CliError, formatDate, info, printJson, progressReporter, table } from './output.ts';
import { ask, readStdin } from './prompt.ts';
import { downloadTo, expandPaths, fileSource } from './transfer.ts';

const program = new Command();

program
  .name('fles')
  .description('Envia e recebe arquivos criptografados entre o computador e o celular (Fles).')
  .version('1.0.0')
  .showHelpAfterError();

function fileRow(file: FileItem, folderPath: string): string[] {
  return [`#${file.id}`, file.name ?? '🔒', formatBytes(file.size), formatDate(file.createdAt), folderPath];
}

function fileJson(file: FileItem, folderPath?: string) {
  return { id: file.id, name: file.name, size: file.size, mimeType: file.mimeType, kind: file.kind, createdAt: file.createdAt, folderId: file.folderId, folder: folderPath };
}

program
  .command('login')
  .description('Conecta este computador à sua conta (cria um token de API só para arquivos).')
  .option('-s, --server <url>', 'endereço da API (ex.: https://manage.seudominio.com)')
  .option('-e, --email <email>', 'e-mail da conta')
  .option('-t, --token <token>', 'usar um token de API já criado (manage_...)')
  .action(async (options: { server?: string; email?: string; token?: string }) => {
    const config = await loadConfig();
    const server = normalizeBaseUrl(options.server ?? config.server ?? (await ask('Servidor da API: ')));
    let token = options.token;
    if (!token) {
      const email = options.email ?? (await ask('E-mail: '));
      const password = await ask('Senha: ', { hidden: true });
      const session = await login(server, email, password);
      const sessionApi = new ApiClient({ baseUrl: server, credentials: { kind: 'bearer', token: session.token } });
      const created = await createApiToken(sessionApi, `CLI (${hostname()})`);
      token = created.plainTextToken;
      await logout(sessionApi).catch(() => undefined);
    }
    const api = new ApiClient({ baseUrl: server, credentials: { kind: 'apiToken', token } });
    await api.get('channels');
    await updateConfig((stored) => {
      stored.server = server;
      stored.token = token;
    });
    info('✓ Conectado.');
    if (!effective(await loadConfig()).key) info('Agora configure a chave de criptografia com: fles pair');
  });

program
  .command('logout')
  .description('Remove o token e a chave salvos neste computador.')
  .action(async () => {
    await updateConfig((config) => {
      delete config.token;
      delete config.key;
    });
    info('✓ Token e chave removidos deste computador. Revogue o token no app web se quiser invalidá-lo.');
  });

program
  .command('pair')
  .description('Recebe a chave de criptografia do celular digitando um código no app.')
  .action(async () => {
    const context = await Context.open({ needsKey: false });
    const request = await PairingRequest.start(context.service.api, `CLI (${hostname()})`);
    info('\nNo celular, abra o Fles → Ajustes → Conectar computador e digite o código:\n');
    info(`    ${request.code}\n`);
    info('Aguardando o celular… (Ctrl+C para cancelar)');
    const abort = new AbortController();
    process.once('SIGINT', () => {
      abort.abort();
      void request.cancel().finally(() => process.exit(130));
    });
    const key = await request.waitForKey(abort.signal);
    await updateConfig((config) => {
      config.key = key.encoded;
    });
    info(`✓ Chave recebida. Impressão digital: ${await key.fingerprint()}`);
  });

const keyCommand = program.command('key').description('Gerencia a chave de criptografia local.');

keyCommand
  .command('set [key]')
  .description('Define a chave colando o valor em base64 (mostrado no app em Ajustes → Chave).')
  .action(async (value?: string) => {
    const key = FlesKey.fromBase64(value ?? (await ask('Chave (base64): ', { hidden: true })));
    await updateConfig((config) => {
      config.key = key.encoded;
    });
    info(`✓ Chave salva. Impressão digital: ${await key.fingerprint()}`);
  });

keyCommand
  .command('show')
  .description('Mostra a impressão digital da chave (para conferir com o app).')
  .action(async () => {
    const { key } = effective(await loadConfig());
    if (!key) throw new CliError('Nenhuma chave configurada.');
    process.stdout.write(`${await FlesKey.fromBase64(key).fingerprint()}\n`);
  });

program
  .command('status')
  .description('Mostra servidor, conexão e chave configurados.')
  .action(async () => {
    const settings = effective(await loadConfig());
    info(`Servidor:      ${settings.server ?? '(não configurado)'}`);
    info(`Token:         ${settings.token ? `${settings.token.slice(0, 12)}…` : '(não configurado)'}`);
    info(`Chave:         ${settings.key ? await FlesKey.fromBase64(settings.key).fingerprint() : '(não configurada)'}`);
    info(`Pasta padrão:  ${settings.defaultFolder}`);
    if (settings.server && settings.token) {
      const context = await Context.open({ needsKey: false });
      const folders = await context.service.folders();
      const readable = folders.filter((folder) => folder.name !== null).length;
      info(`Conexão:       ok (${folders.length} pastas${settings.key ? `, ${readable} legíveis com esta chave` : ''})`);
    }
  });

program
  .command('folders')
  .alias('pastas')
  .description('Lista as pastas.')
  .option('--json', 'saída em JSON')
  .action(async (options: { json?: boolean }) => {
    const context = await Context.open();
    const { folders, files } = await context.load();
    const counts = new Map<string, number>();
    for (const file of files) counts.set(file.folderId, (counts.get(file.folderId) ?? 0) + 1);
    const rows = folders
      .map((folder) => ({ folder, path: context.folderPath(folder, folders) }))
      .sort((a, b) => a.path.localeCompare(b.path, 'pt-BR'));
    if (options.json) {
      printJson(rows.map(({ folder, path }) => ({ id: folder.id, name: folder.name, path, parentId: folder.parentId, isSecure: folder.isSecure, files: counts.get(folder.id) ?? 0 })));
      return;
    }
    if (rows.length === 0) return info('Nenhuma pasta ainda.');
    process.stdout.write(`${table(rows.map(({ folder, path }) => [`#${folder.id}`, folder.isSecure ? `🔒 ${path}` : path, `${counts.get(folder.id) ?? 0} arquivos`]))}\n`);
  });

program
  .command('ls [folder]')
  .description('Lista os arquivos de uma pasta (padrão: pasta de transferências). Use --all para todos.')
  .option('-a, --all', 'todos os arquivos de todas as pastas')
  .option('-n, --limit <n>', 'máximo de linhas', (value) => Number.parseInt(value, 10))
  .option('--json', 'saída em JSON')
  .action(async (folderReference: string | undefined, options: { all?: boolean; limit?: number; json?: boolean }) => {
    const context = await Context.open();
    const { folders, files } = await context.load();
    let scope = files;
    if (!options.all) {
      const folder = folderReference ? await context.requireFolder(folderReference) : await context.findFolder(context.defaultFolderName);
      if (!folder) return info(`A pasta "${context.defaultFolderName}" ainda não existe. Envie algo com: fles send <arquivo>`);
      scope = files.filter((file) => file.folderId === folder.id);
    }
    if (options.limit) scope = scope.slice(0, options.limit);
    const pathOf = (file: FileItem) => {
      const folder = folders.find((item) => item.id === file.folderId);
      return folder ? context.folderPath(folder, folders) : '';
    };
    if (options.json) return printJson(scope.map((file) => fileJson(file, pathOf(file))));
    if (scope.length === 0) return info('Nenhum arquivo.');
    process.stdout.write(`${table(scope.map((file) => fileRow(file, pathOf(file))))}\n`);
  });

program
  .command('send <paths...>')
  .alias('enviar')
  .description('Envia arquivos para o celular (padrão: pasta de transferências).')
  .option('-t, --to <folder>', 'pasta de destino (nome, caminho Pai/Filha ou #id)')
  .option('-m, --message <text>', 'texto junto com o arquivo')
  .option('-r, --recursive', 'enviar o conteúdo de pastas')
  .option('--json', 'saída em JSON')
  .action(async (paths: string[], options: { to?: string; message?: string; recursive?: boolean; json?: boolean }) => {
    const context = await Context.open();
    const files = await expandPaths(paths, Boolean(options.recursive));
    const folder = await context.targetFolder(options.to);
    const sent: unknown[] = [];
    for (const path of files) {
      const source = await fileSource(path);
      const progress = options.json ? null : progressReporter(source.name);
      try {
        const message = await context.service.sendFile(folder, source, { onProgress: progress?.update, text: options.message });
        progress?.done(`→ ${folder.name}`);
        sent.push(...message.attachments.map((file) => fileJson(file, folder.name ?? undefined)));
      } finally {
        await source.close();
      }
    }
    if (options.json) printJson(sent);
  });

program
  .command('note [text...]')
  .alias('nota')
  .description('Envia um texto (use "-" para ler da entrada padrão).')
  .option('-t, --to <folder>', 'pasta de destino')
  .action(async (words: string[], options: { to?: string }) => {
    let text = words.join(' ');
    if (!text || text === '-') text = await readStdin();
    if (!text.trim()) throw new CliError('Texto vazio.');
    const context = await Context.open();
    const folder = await context.targetFolder(options.to);
    await context.service.sendNote(folder, text.replace(/\n$/, ''));
    info(`✓ Nota enviada → ${folder.name}`);
  });

program
  .command('get [files...]')
  .alias('baixar')
  .description('Baixa arquivos pelo nome ou #id.')
  .option('-f, --folder <folder>', 'procurar só nesta pasta')
  .option('-o, --out <dir>', 'pasta de saída', '.')
  .option('-l, --latest [n]', 'baixar os N arquivos mais recentes da pasta', (value) => Number.parseInt(value, 10))
  .option('--json', 'saída em JSON')
  .action(async (references: string[], options: { folder?: string; out: string; latest?: number | boolean; json?: boolean }) => {
    const context = await Context.open();
    const { folders, files } = await context.load();
    let selected: FileItem[];
    if (options.latest) {
      const folder = options.folder ? await context.requireFolder(options.folder) : await context.findFolder(context.defaultFolderName);
      if (!folder) throw new CliError('Pasta não encontrada.');
      const count = typeof options.latest === 'number' && options.latest > 0 ? options.latest : 1;
      selected = files.filter((file) => file.folderId === folder.id).slice(0, count);
    } else {
      if (references.length === 0) throw new CliError('Informe arquivos ou use --latest.');
      selected = await context.resolveFiles(references, options.folder);
    }
    if (selected.length === 0) return info('Nenhum arquivo para baixar.');
    const directory = resolve(options.out);
    await mkdir(directory, { recursive: true });
    const saved: unknown[] = [];
    for (const file of selected) {
      const path = await downloadTo(context.service, file, context.isSecure(file.folderId, folders), directory, options.json);
      saved.push({ ...fileJson(file), path });
    }
    if (options.json) printJson(saved);
  });

program
  .command('pull [folder]')
  .alias('receber')
  .description('Baixa o que chegou na pasta desde o último pull (padrão: pasta de transferências).')
  .option('-o, --out <dir>', 'pasta de saída', '.')
  .option('--delete', 'apagar do servidor depois de baixar (mover para o PC)')
  .option('-w, --watch [seconds]', 'continuar verificando a cada N segundos (padrão 10)', (value) => Number.parseInt(value, 10))
  .option('--json', 'saída em JSON (uma linha por arquivo)')
  .action(async (folderReference: string | undefined, options: { out: string; delete?: boolean; watch?: number | boolean; json?: boolean }) => {
    const directory = resolve(options.out);
    await mkdir(directory, { recursive: true });
    const interval = typeof options.watch === 'number' && options.watch > 0 ? options.watch : 10;
    const runOnce = async (): Promise<number> => {
      const context = await Context.open();
      const folder = folderReference ? await context.requireFolder(folderReference) : await context.targetFolder(undefined);
      const { files } = await context.load();
      const stateKey = `${effective(context.config).server}#${folder.id}`;
      const pulled = new Set(context.config.pulled?.[stateKey] ?? []);
      const pending = files.filter((file) => file.folderId === folder.id && !pulled.has(file.id)).reverse();
      for (const file of pending) {
        const path = await downloadTo(context.service, file, folder.isSecure, directory, options.json);
        if (options.delete) await context.service.deleteFiles([file.id]);
        pulled.add(file.id);
        const current = new Set(files.filter((item) => item.folderId === folder.id).map((item) => item.id));
        await updateConfig((config) => {
          config.pulled ??= {};
          config.pulled[stateKey] = [...pulled].filter((id) => current.has(id) || id === file.id);
        });
        if (options.json) process.stdout.write(`${JSON.stringify({ ...fileJson(file, folder.name ?? undefined), path })}\n`);
      }
      return pending.length;
    };
    if (!options.watch) {
      const count = await runOnce();
      if (count === 0 && !options.json) info('Nada novo.');
      return;
    }
    info(`Observando a cada ${interval}s… (Ctrl+C para parar)`);
    for (;;) {
      try {
        await runOnce();
      } catch (error) {
        info(`! ${describeError(error)}`);
      }
      await new Promise((done) => setTimeout(done, interval * 1000));
    }
  });

program
  .command('rm <files...>')
  .description('Apaga arquivos do servidor pelo nome ou #id.')
  .option('-f, --folder <folder>', 'procurar só nesta pasta')
  .action(async (references: string[], options: { folder?: string }) => {
    const context = await Context.open();
    const files = await context.resolveFiles(references, options.folder);
    await context.service.deleteFiles(files.map((file) => file.id));
    info(`✓ ${files.length} arquivo(s) apagado(s).`);
  });

program
  .command('mkdir <name>')
  .description('Cria uma pasta.')
  .option('-p, --parent <folder>', 'pasta pai')
  .action(async (name: string, options: { parent?: string }) => {
    const context = await Context.open();
    const parent = options.parent ? await context.requireFolder(options.parent) : null;
    const folder = await context.service.createFolder(name, { parentId: parent?.id ?? null, isSecure: parent?.isSecure ?? false });
    info(`✓ Pasta criada: ${folder.name} (#${folder.id})`);
  });

program
  .command('config')
  .description('Ajusta preferências.')
  .addOption(new Option('--default-folder <name>', 'pasta usada por send/pull/ls quando nenhuma é informada'))
  .addOption(new Option('--server <url>', 'endereço da API'))
  .action(async (options: { defaultFolder?: string; server?: string }) => {
    const config = await updateConfig((stored) => {
      if (options.defaultFolder) stored.defaultFolder = options.defaultFolder;
      if (options.server) stored.server = normalizeBaseUrl(options.server);
    });
    const settings = effective(config);
    info(`Servidor: ${settings.server ?? '(não configurado)'}\nPasta padrão: ${settings.defaultFolder}`);
  });

program.parseAsync().catch((error: unknown) => {
  if (error instanceof Error && error.name === 'AbortError') process.exit(130);
  info(`✗ ${describeError(error)}`);
  process.exit(1);
});
