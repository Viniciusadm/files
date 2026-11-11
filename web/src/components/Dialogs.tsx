import { createApiToken, listApiTokens, revokeApiToken, type ApiClient, type ApiTokenInfo, type Folder, type FlesKey } from '@fles/core';
import { Copy, Eye, EyeOff, Folder as FolderIcon, FolderLock, LogOut, Terminal, Trash2 } from 'lucide-react';
import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { formatDate } from './fileVisuals.tsx';
import { Button, IconButton, Modal, Spinner, TextField, toast, toastError } from './ui.tsx';

export function PromptDialog({ title, label, initial = '', confirm, onSubmit, onClose }: { title: string; label: string; initial?: string; confirm: string; onSubmit: (value: string) => Promise<void>; onClose: () => void }) {
  const [value, setValue] = useState(initial);
  const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!value.trim()) return;
    setBusy(true);
    try {
      await onSubmit(value.trim());
      onClose();
    } catch (error) {
      toastError(error);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal title={title} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        <TextField
          label={label}
          value={value}
          onChange={(event) => setValue(event.target.value)}
          onFocus={(event) => {
            const dot = event.target.value.lastIndexOf('.');
            event.target.setSelectionRange(0, dot > 0 ? dot : event.target.value.length);
          }}
        />
        <div className="flex justify-end gap-2">
          <Button type="button" onClick={onClose}>Cancelar</Button>
          <Button type="submit" variant="primary" busy={busy} disabled={!value.trim()}>{confirm}</Button>
        </div>
      </form>
    </Modal>
  );
}

export function ConfirmDialog({ title, message, confirm, onConfirm, onClose }: { title: string; message: React.ReactNode; confirm: string; onConfirm: () => Promise<void>; onClose: () => void }) {
  const [busy, setBusy] = useState(false);
  return (
    <Modal
      title={title}
      onClose={onClose}
      footer={
        <>
          <Button onClick={onClose}>Cancelar</Button>
          <Button
            variant="danger"
            busy={busy}
            data-autofocus
            onClick={async () => {
              setBusy(true);
              try {
                await onConfirm();
                onClose();
              } catch (error) {
                toastError(error);
              } finally {
                setBusy(false);
              }
            }}
          >
            {confirm}
          </Button>
        </>
      }
    >
      <p className="text-sm text-muted">{message}</p>
    </Modal>
  );
}

export function MoveDialog({ folders, secure, currentFolderId, onMove, onClose }: { folders: Folder[]; secure: boolean; currentFolderId: string | null; onMove: (folder: Folder) => Promise<void>; onClose: () => void }) {
  const [query, setQuery] = useState('');
  const [busy, setBusy] = useState<string | null>(null);
  const options = useMemo(
    () => folders
      .filter((folder) => folder.isSecure === secure && folder.name !== null && folder.id !== currentFolderId)
      .filter((folder) => (folder.name ?? '').toLowerCase().includes(query.trim().toLowerCase()))
      .sort((a, b) => (a.name ?? '').localeCompare(b.name ?? '', 'pt-BR')),
    [folders, secure, currentFolderId, query],
  );
  return (
    <Modal title="Mover para…" onClose={onClose}>
      <TextField placeholder="Buscar pasta" value={query} onChange={(event) => setQuery(event.target.value)} />
      <div className="mt-3 max-h-80 space-y-0.5 overflow-y-auto">
        {options.map((folder) => {
          const Icon = folder.isSecure ? FolderLock : FolderIcon;
          return (
            <button
              key={folder.id}
              disabled={busy !== null}
              onClick={async () => {
                setBusy(folder.id);
                try {
                  await onMove(folder);
                  onClose();
                } catch (error) {
                  toastError(error);
                  setBusy(null);
                }
              }}
              className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left text-sm hover:bg-sunken disabled:opacity-50"
            >
              <Icon className="size-4" style={{ color: folder.color }} />
              <span className="flex-1 truncate">{folder.name}</span>
              {busy === folder.id ? <Spinner className="size-4" /> : null}
            </button>
          );
        })}
        {options.length === 0 ? <p className="px-3 py-4 text-sm text-muted">Nenhuma pasta disponível{secure ? ' (só é possível mover entre pastas seguras)' : ''}.</p> : null}
      </div>
    </Modal>
  );
}

export function PinDialog({ onUnlock, onClose }: { onUnlock: (pin: string) => Promise<void>; onClose: () => void }) {
  const [pin, setPin] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  async function submit(value: string) {
    setBusy(true);
    setError(null);
    try {
      await onUnlock(value);
      onClose();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : String(caught));
      setPin('');
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal title="Pastas seguras" onClose={onClose}>
      <p className="mb-4 text-sm text-muted">Digite o PIN de 4 dígitos que você usa no celular.</p>
      <input
        autoFocus
        inputMode="numeric"
        autoComplete="off"
        maxLength={4}
        value={pin}
        disabled={busy}
        onChange={(event) => {
          const value = event.target.value.replace(/\D/g, '').slice(0, 4);
          setPin(value);
          if (value.length === 4) void submit(value);
        }}
        className="h-14 w-full rounded-xl border border-line bg-sunken text-center font-mono text-3xl tracking-[0.6em] outline-none focus:border-primary"
        aria-label="PIN"
      />
      <div className="mt-3 flex h-5 items-center justify-center text-sm">
        {busy ? <Spinner className="size-4" /> : error ? <span className="text-danger">{error}</span> : null}
      </div>
    </Modal>
  );
}

function TokenList({ api }: { api: ApiClient }) {
  const [tokens, setTokens] = useState<ApiTokenInfo[] | null>(null);
  const [created, setCreated] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const reload = () => listApiTokens(api).then(setTokens, toastError);
  useEffect(() => {
    void reload();
  }, []);
  const command = created ? `fles login --server ${api.baseUrl.replace(/\/$/, '')} --token ${created}` : '';
  return (
    <div className="space-y-3">
      {created ? (
        <div className="rounded-xl border border-primary/40 bg-primary-soft p-3">
          <p className="mb-2 text-xs font-medium">Copie agora, o token não será mostrado de novo:</p>
          <div className="flex items-center gap-2">
            <code className="min-w-0 flex-1 truncate rounded-md bg-surface px-2 py-1.5 font-mono text-xs">{command}</code>
            <IconButton label="Copiar comando" onClick={() => navigator.clipboard.writeText(command).then(() => toast('Comando copiado'))}>
              <Copy className="size-4" />
            </IconButton>
          </div>
        </div>
      ) : null}
      {tokens === null ? <Spinner /> : null}
      <ul className="divide-y divide-line rounded-xl border border-line">
        {tokens?.filter((token) => token.status !== 'revoked').map((token) => (
          <li key={token.id} className="flex items-center gap-3 px-3 py-2">
            <Terminal className="size-4 text-muted" />
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm">{token.name}</p>
              <p className="text-xs text-muted">{token.lastUsedAt ? `Usado ${formatDate(token.lastUsedAt)}` : 'Nunca usado'}</p>
            </div>
            <IconButton
              label="Revogar"
              className="size-8 hover:text-danger"
              onClick={async () => {
                await revokeApiToken(api, token.id).catch(toastError);
                void reload();
              }}
            >
              <Trash2 className="size-4" />
            </IconButton>
          </li>
        ))}
        {tokens && tokens.filter((token) => token.status !== 'revoked').length === 0 ? <li className="px-3 py-3 text-sm text-muted">Nenhum token ativo.</li> : null}
      </ul>
      <Button
        busy={busy}
        onClick={async () => {
          setBusy(true);
          try {
            const token = await createApiToken(api, 'CLI');
            setCreated(token.plainTextToken);
            void reload();
          } catch (error) {
            toastError(error);
          } finally {
            setBusy(false);
          }
        }}
      >
        <Terminal className="size-4" /> Criar token para o CLI
      </Button>
    </div>
  );
}

export function SettingsDialog({ api, flesKey, email, onLogout, onClose }: { api: ApiClient; flesKey: FlesKey; email: string | null; onLogout: () => void; onClose: () => void }) {
  const [fingerprint, setFingerprint] = useState('');
  const [reveal, setReveal] = useState(false);
  useEffect(() => {
    void flesKey.fingerprint().then(setFingerprint);
  }, [flesKey]);
  return (
    <Modal title="Ajustes" onClose={onClose} wide>
      <div className="space-y-6">
        <section>
          <h3 className="mb-2 text-sm font-semibold">Conta</h3>
          <div className="flex items-center justify-between gap-3 rounded-xl border border-line px-3 py-2.5">
            <div className="min-w-0">
              <p className="truncate text-sm">{email ?? 'Conectado'}</p>
              <p className="truncate text-xs text-muted">{api.baseUrl}</p>
            </div>
            <Button variant="ghost" onClick={onLogout}>
              <LogOut className="size-4" /> Sair
            </Button>
          </div>
        </section>
        <section>
          <h3 className="mb-1 text-sm font-semibold">Chave de criptografia</h3>
          <p className="mb-2 text-xs text-muted">Confira se a impressão digital é a mesma mostrada no celular. A chave fica só neste navegador.</p>
          <div className="rounded-xl border border-line px-3 py-2.5">
            <p className="font-mono text-sm">{fingerprint}</p>
            {reveal ? <p className="mt-2 break-all rounded-md bg-sunken px-2 py-1.5 font-mono text-xs select-all">{flesKey.encoded}</p> : null}
            <div className="mt-2 flex gap-2">
              <Button variant="ghost" onClick={() => setReveal((value) => !value)}>
                {reveal ? <EyeOff className="size-4" /> : <Eye className="size-4" />} {reveal ? 'Ocultar' : 'Mostrar chave'}
              </Button>
              {reveal ? (
                <Button variant="ghost" onClick={() => navigator.clipboard.writeText(flesKey.encoded).then(() => toast('Chave copiada'))}>
                  <Copy className="size-4" /> Copiar
                </Button>
              ) : null}
            </div>
          </div>
        </section>
        <section>
          <h3 className="mb-1 text-sm font-semibold">Linha de comando</h3>
          <p className="mb-3 text-xs text-muted">
            Use o <code className="font-mono">fles</code> para automatizar: <code className="font-mono">fles send arquivo.pdf</code>, <code className="font-mono">fles pull --watch</code>. Cada computador tem o seu token, que só acessa os arquivos.
          </p>
          <TokenList api={api} />
        </section>
      </div>
    </Modal>
  );
}
