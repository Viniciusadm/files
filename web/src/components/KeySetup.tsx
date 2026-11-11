import { ApiClient, RelayKey, PairingRequest } from '@relay/core';
import { ClipboardPaste, KeyRound, Smartphone } from 'lucide-react';
import { useEffect, useState, type FormEvent } from 'react';
import { AuthShell } from './Brand.tsx';
import { Button, Spinner, TextField } from './ui.tsx';

type Mode = 'pair' | 'paste';

function deviceName(): string {
  const agent = navigator.userAgent;
  const browser = /Firefox\//.test(agent) ? 'Firefox' : /Edg\//.test(agent) ? 'Edge' : /Chrome\//.test(agent) ? 'Chrome' : /Safari\//.test(agent) ? 'Safari' : 'Navegador';
  const system = /Windows/.test(agent) ? 'Windows' : /Mac OS/.test(agent) ? 'macOS' : /Linux/.test(agent) ? 'Linux' : '';
  return `${browser}${system ? ` no ${system}` : ''}`;
}

function PairPanel({ api, onKey }: { api: ApiClient; onKey: (key: RelayKey) => void }) {
  const [request, setRequest] = useState<PairingRequest | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let current: PairingRequest | null = null;
    setError(null);
    setRequest(null);
    const timer = setTimeout(() => PairingRequest.start(api, deviceName())
      .then(async (started) => {
        if (controller.signal.aborted) {
          void started.cancel();
          return;
        }
        current = started;
        setRequest(started);
        onKey(await started.waitForKey(controller.signal));
      })
      .catch((caught: unknown) => {
        if (!controller.signal.aborted) setError(caught instanceof Error ? caught.message : String(caught));
      }), 50);
    return () => {
      clearTimeout(timer);
      controller.abort();
      void current?.cancel();
    };
  }, [api, attempt, onKey]);

  return (
    <div className="text-center">
      <p className="text-sm text-muted">
        No celular, abra o Relay → <strong className="text-ink">Ajustes</strong> → <strong className="text-ink">Conectar computador</strong> e digite:
      </p>
      <div className="my-6 flex min-h-16 items-center justify-center">
        {request ? (
          <span className="select-all rounded-xl bg-sunken px-5 py-3 font-mono text-3xl font-semibold tracking-[0.2em]">{request.code}</span>
        ) : error ? null : (
          <Spinner className="size-6" />
        )}
      </div>
      {error ? (
        <div className="space-y-3">
          <p className="rounded-lg bg-danger-soft px-3 py-2 text-sm text-danger">{error}</p>
          <Button onClick={() => setAttempt((value) => value + 1)}>Gerar novo código</Button>
        </div>
      ) : (
        <p className="flex items-center justify-center gap-2 text-xs text-muted">
          <Spinner className="size-3.5" /> Aguardando o celular… o código vale por 10 minutos.
        </p>
      )}
    </div>
  );
}

function PastePanel({ onKey }: { onKey: (key: RelayKey) => void }) {
  const [value, setValue] = useState('');
  const [error, setError] = useState<string | null>(null);

  function submit(event: FormEvent) {
    event.preventDefault();
    try {
      onKey(RelayKey.fromBase64(value));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : String(caught));
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <TextField
        label="Chave E2EE (base64)"
        value={value}
        onChange={(event) => setValue(event.target.value)}
        placeholder="Ex.: q1w2e3…="
        autoComplete="off"
        spellCheck={false}
        hint="No celular: Ajustes → Chave E2EE. A chave fica só neste navegador e nunca vai para o servidor."
      />
      {error ? <p className="rounded-lg bg-danger-soft px-3 py-2 text-sm text-danger">{error}</p> : null}
      <Button variant="primary" type="submit" className="w-full h-10" disabled={!value.trim()}>
        <KeyRound className="size-4" /> Usar esta chave
      </Button>
    </form>
  );
}

export function KeySetup({ api, onKey, onLogout }: { api: ApiClient; onKey: (key: RelayKey) => void; onLogout: () => void }) {
  const [mode, setMode] = useState<Mode>('pair');
  const tabs: { id: Mode; label: string; icon: React.ReactNode }[] = [
    { id: 'pair', label: 'Parear com o celular', icon: <Smartphone className="size-4" /> },
    { id: 'paste', label: 'Colar chave', icon: <ClipboardPaste className="size-4" /> },
  ];
  return (
    <AuthShell subtitle="Seus arquivos são criptografados no celular. Traga a chave para este computador.">
      <div className="mb-6 grid grid-cols-2 gap-1 rounded-xl bg-sunken p-1">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setMode(tab.id)}
            className={`flex items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition ${mode === tab.id ? 'bg-surface shadow-sm' : 'text-muted hover:text-ink'}`}
          >
            {tab.icon}
            {tab.label}
          </button>
        ))}
      </div>
      {mode === 'pair' ? <PairPanel api={api} onKey={onKey} /> : <PastePanel onKey={onKey} />}
      <button onClick={onLogout} className="mt-6 block w-full text-center text-xs text-muted hover:text-ink">
        Sair desta conta
      </button>
    </AuthShell>
  );
}
