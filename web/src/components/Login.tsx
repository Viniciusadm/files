import { login, normalizeBaseUrl } from '@relay/core';
import { ChevronDown } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { DEFAULT_SERVER, loadSession, type StoredSession } from '../lib/storage.ts';
import { AuthShell } from './Brand.tsx';
import { Button, TextField } from './ui.tsx';

export function Login({ onLogin }: { onLogin: (session: StoredSession) => void }) {
  const previous = loadSession();
  const [server, setServer] = useState(previous?.server ?? DEFAULT_SERVER);
  const [email, setEmail] = useState(previous?.email ?? '');
  const [password, setPassword] = useState('');
  const [showServer, setShowServer] = useState(!import.meta.env.VITE_API_URL);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const base = normalizeBaseUrl(server);
      const result = await login(base, email, password);
      onLogin({ server: base, token: result.token, email: result.user?.email ?? email.trim(), key: previous?.key ?? null });
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : String(caught));
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthShell subtitle="Seus arquivos, do celular para o computador.">
      <form onSubmit={submit} className="space-y-4">
        <TextField label="E-mail" type="email" autoComplete="username" required value={email} onChange={(event) => setEmail(event.target.value)} autoFocus />
        <TextField label="Senha" type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} />
        {showServer ? (
          <TextField label="Servidor" value={server} onChange={(event) => setServer(event.target.value)} hint="Endereço da API do Manage, o mesmo usado no app." />
        ) : (
          <button type="button" onClick={() => setShowServer(true)} className="inline-flex items-center gap-1 text-xs text-muted hover:text-ink">
            Servidor: {server} <ChevronDown className="size-3" />
          </button>
        )}
        {error ? <p className="rounded-lg bg-danger-soft px-3 py-2 text-sm text-danger">{error}</p> : null}
        <Button variant="primary" type="submit" busy={busy} className="w-full h-10">
          Entrar
        </Button>
      </form>
    </AuthShell>
  );
}
