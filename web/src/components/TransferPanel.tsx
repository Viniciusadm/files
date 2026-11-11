import { formatBytes } from '@relay/core';
import { ArrowDown, ArrowUp, CheckCircle2, ChevronDown, RotateCcw, X, XCircle } from 'lucide-react';
import { useState } from 'react';
import { useStore } from '../lib/store.ts';
import type { Transfer, Transfers } from '../lib/transfers.ts';
import { IconButton } from './ui.tsx';

function speed(item: Transfer): string {
  if (!item.startedAt || item.status !== 'running') return '';
  const seconds = (Date.now() - item.startedAt) / 1000;
  return seconds > 1 ? `${formatBytes(item.done / seconds)}/s` : '';
}

function Row({ item, transfers }: { item: Transfer; transfers: Transfers }) {
  const percent = item.total > 0 ? Math.min(100, (item.done / item.total) * 100) : 0;
  const Direction = item.direction === 'upload' ? ArrowUp : ArrowDown;
  return (
    <li className="px-4 py-2.5">
      <div className="flex items-center gap-3">
        <span className={`flex size-7 shrink-0 items-center justify-center rounded-full ${item.status === 'error' ? 'bg-danger-soft text-danger' : 'bg-primary-soft text-primary'}`}>
          {item.status === 'done' ? <CheckCircle2 className="size-4 text-success" /> : item.status === 'error' ? <XCircle className="size-4" /> : <Direction className="size-4" />}
        </span>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium">{item.name}</p>
          <p className={`truncate text-xs ${item.status === 'error' ? 'text-danger' : 'text-muted'}`}>
            {item.status === 'queued' && 'Na fila'}
            {item.status === 'running' && `${formatBytes(item.done)} de ${formatBytes(item.total)} ${speed(item)}`}
            {item.status === 'done' && (item.direction === 'upload' ? `Enviado${item.folderName ? ` para ${item.folderName}` : ''}` : 'Baixado')}
            {item.status === 'cancelled' && 'Cancelado'}
            {item.status === 'error' && item.error}
          </p>
        </div>
        {item.status === 'error' || item.status === 'cancelled' ? (
          <IconButton label="Tentar de novo" className="size-7" onClick={() => transfers.retry(item.id)}>
            <RotateCcw className="size-3.5" />
          </IconButton>
        ) : null}
        {item.status === 'queued' || item.status === 'running' ? (
          <IconButton label="Cancelar" className="size-7" onClick={() => transfers.cancel(item.id)}>
            <X className="size-3.5" />
          </IconButton>
        ) : null}
      </div>
      {item.status === 'running' ? (
        <div className="mt-2 ml-10 h-1 overflow-hidden rounded-full bg-sunken">
          <div className="h-full rounded-full bg-primary transition-[width] duration-200" style={{ width: `${percent}%` }} />
        </div>
      ) : null}
    </li>
  );
}

export function TransferPanel({ transfers }: { transfers: Transfers }) {
  const { items } = useStore(transfers.store);
  const [collapsed, setCollapsed] = useState(false);
  if (items.length === 0) return null;
  const active = items.filter((item) => item.status === 'queued' || item.status === 'running').length;
  const failed = items.filter((item) => item.status === 'error').length;
  const title = active > 0 ? `${active} transferência${active > 1 ? 's' : ''} em andamento` : failed > 0 ? `${failed} com erro` : 'Transferências concluídas';
  return (
    <section className="animate-in fixed right-4 bottom-4 z-40 w-[min(380px,calc(100vw-2rem))] overflow-hidden rounded-2xl border border-line bg-surface shadow-2xl">
      <header className="flex items-center gap-2 border-b border-line py-2 pr-2 pl-4">
        <h3 className="flex-1 text-sm font-semibold">{title}</h3>
        <IconButton label={collapsed ? 'Expandir' : 'Recolher'} className="size-8" onClick={() => setCollapsed((value) => !value)}>
          <ChevronDown className={`size-4 transition ${collapsed ? 'rotate-180' : ''}`} />
        </IconButton>
        {active === 0 ? (
          <IconButton label="Fechar" className="size-8" onClick={() => transfers.clearFinished()}>
            <X className="size-4" />
          </IconButton>
        ) : null}
      </header>
      {collapsed ? null : (
        <ul className="max-h-72 divide-y divide-line overflow-y-auto">
          {items.slice().reverse().map((item) => (
            <Row key={item.id} item={item} transfers={transfers} />
          ))}
        </ul>
      )}
    </section>
  );
}
