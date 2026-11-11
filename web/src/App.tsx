import { ApiClient, RelayKey, RelayService, logout } from '@relay/core';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { KeySetup } from './components/KeySetup.tsx';
import { Login } from './components/Login.tsx';
import { Toasts, toast } from './components/ui.tsx';
import { Workspace } from './components/Workspace.tsx';
import { Library } from './lib/library.ts';
import { MediaCache } from './lib/media.ts';
import { loadSession, saveSession, type StoredSession } from './lib/storage.ts';
import { Transfers } from './lib/transfers.ts';

function Session({ session, onChange, onExpired }: { session: StoredSession; onChange: (session: StoredSession | null) => void; onExpired: () => void }) {
  const api = useMemo(
    () => new ApiClient({ baseUrl: session.server, credentials: { kind: 'bearer', token: session.token }, onUnauthorized: onExpired }),
    [session.server, session.token, onExpired],
  );
  const relayKey = useMemo(() => (session.key ? RelayKey.fromBase64(session.key) : null), [session.key]);
  const runtime = useMemo(() => {
    if (!relayKey) return null;
    const service = new RelayService(api, relayKey);
    const library = new Library(service);
    const transfers = new Transfers(service, (message) => library.upsertLocally(message));
    const media = new MediaCache(service);
    return { service, library, transfers, media };
  }, [api, relayKey]);

  useEffect(() => () => runtime?.media.clear(), [runtime]);

  const signOut = useCallback(() => {
    void logout(api).catch(() => undefined);
    onChange(null);
  }, [api, onChange]);

  const acceptKey = useCallback((key: RelayKey) => {
    onChange({ ...session, key: key.encoded });
    void key.fingerprint().then((fingerprint) => toast(`Chave configurada · ${fingerprint}`, 'success'));
  }, [onChange, session]);

  if (!relayKey || !runtime) {
    return <KeySetup api={api} onKey={acceptKey} onLogout={signOut} />;
  }

  return <Workspace api={api} relayKey={relayKey} email={session.email} onLogout={signOut} {...runtime} />;
}

export function App() {
  const [session, setSession] = useState<StoredSession | null>(loadSession);

  const update = useCallback((next: StoredSession | null) => {
    saveSession(next);
    setSession(next);
  }, []);

  const expired = useCallback(() => {
    const current = loadSession();
    if (current) {
      saveSession({ ...current, token: '' });
      toast('Sua sessão expirou. Entre novamente.', 'error');
    }
    setSession((value) => (value ? { ...value, token: '' } : null));
  }, []);

  return (
    <>
      {session?.token ? <Session session={session} onChange={update} onExpired={expired} /> : <Login onLogin={update} />}
      <Toasts />
    </>
  );
}
