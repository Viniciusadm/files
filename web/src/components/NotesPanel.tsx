import { formatBytes, type FileItem, type Message } from '@fles/core';
import { CheckSquare, Copy, Paperclip, SendHorizontal, Square, Trash2 } from 'lucide-react';
import { useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { formatDate } from './fileVisuals.tsx';
import { IconButton, toast } from './ui.tsx';

function Linkified({ text }: { text: string }) {
  const parts = text.split(/(https?:\/\/[^\s]+)/g);
  return (
    <>
      {parts.map((part, index) =>
        /^https?:\/\//.test(part) ? (
          <a key={index} href={part} target="_blank" rel="noreferrer noopener" className="break-all underline decoration-muted underline-offset-2 hover:decoration-ink">
            {part}
          </a>
        ) : (
          <span key={index}>{part}</span>
        ),
      )}
    </>
  );
}

async function copy(text: string) {
  try {
    await navigator.clipboard.writeText(text);
    toast('Copiado');
  } catch {
    toast('Não foi possível copiar', 'error');
  }
}

function NoteCard({ message, onToggleItem, onDelete, onOpenFile }: { message: Message; onToggleItem: (itemId: string) => void; onDelete: () => void; onOpenFile: (file: FileItem) => void }) {
  const copyText = message.listItems.length > 0
    ? [message.text, ...message.listItems.map((item) => `${item.checked ? '[x]' : '[ ]'} ${item.text ?? ''}`)].filter(Boolean).join('\n')
    : message.text ?? '';
  return (
    <article className="group animate-in rounded-xl border border-line bg-surface p-3.5">
      {message.text ? (
        <p className={`text-sm leading-relaxed whitespace-pre-wrap break-words ${message.listItems.length > 0 ? 'mb-2 font-semibold' : ''}`}>
          <Linkified text={message.text} />
        </p>
      ) : message.text === null ? (
        <p className="text-sm text-muted italic">Não foi possível descriptografar.</p>
      ) : null}
      {message.listItems.length > 0 ? (
        <ul className="space-y-1">
          {message.listItems.map((item) => (
            <li key={item.id}>
              <button onClick={() => onToggleItem(item.id)} className="flex w-full items-start gap-2 rounded-md px-1 py-0.5 text-left text-sm hover:bg-sunken">
                {item.checked ? <CheckSquare className="mt-0.5 size-4 shrink-0 text-primary" /> : <Square className="mt-0.5 size-4 shrink-0 text-muted" />}
                <span className={item.checked ? 'text-muted line-through' : ''}>{item.text ?? '…'}</span>
              </button>
            </li>
          ))}
        </ul>
      ) : null}
      {message.attachments.length > 0 ? (
        <div className="mt-2 flex flex-wrap gap-1.5">
          {message.attachments.map((file) => (
            <button key={file.id} onClick={() => onOpenFile(file)} className="inline-flex max-w-full items-center gap-1.5 rounded-lg bg-sunken px-2 py-1 text-xs hover:bg-primary-soft">
              <Paperclip className="size-3.5 shrink-0" />
              <span className="truncate">{file.name ?? 'arquivo'}</span>
              <span className="shrink-0 text-muted">{formatBytes(file.size)}</span>
            </button>
          ))}
        </div>
      ) : null}
      <div className="mt-2 flex items-center justify-between">
        <time className="text-xs text-muted">{formatDate(message.createdAt)}</time>
        <div className="flex opacity-0 transition group-hover:opacity-100 focus-within:opacity-100">
          {copyText ? (
            <IconButton label="Copiar texto" className="size-7" onClick={() => void copy(copyText)}>
              <Copy className="size-3.5" />
            </IconButton>
          ) : null}
          <IconButton label="Apagar" className="size-7 hover:text-danger" onClick={onDelete}>
            <Trash2 className="size-3.5" />
          </IconButton>
        </div>
      </div>
    </article>
  );
}

export function NotesPanel({
  messages,
  onSend,
  onToggleItem,
  onDelete,
  onOpenFile,
  disabled,
}: {
  messages: Message[];
  onSend: (text: string) => Promise<void>;
  onToggleItem: (message: Message, itemId: string) => void;
  onDelete: (message: Message) => void;
  onOpenFile: (file: FileItem) => void;
  disabled?: boolean;
}) {
  const [draft, setDraft] = useState('');
  const [busy, setBusy] = useState(false);
  const area = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    const element = area.current;
    if (!element) return;
    element.style.height = 'auto';
    element.style.height = `${Math.min(element.scrollHeight, 220)}px`;
  }, [draft]);

  async function send() {
    const text = draft.trim();
    if (!text || busy) return;
    setBusy(true);
    try {
      await onSend(text);
      setDraft('');
    } finally {
      setBusy(false);
    }
  }

  function onKey(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) {
      event.preventDefault();
      void send();
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-end gap-2 rounded-xl border border-line bg-surface p-2 focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/20">
        <textarea
          ref={area}
          rows={1}
          value={draft}
          disabled={disabled}
          onChange={(event) => setDraft(event.target.value)}
          onKeyDown={onKey}
          placeholder="Escreva uma nota ou cole um link para abrir no celular…"
          className="max-h-56 min-h-9 flex-1 resize-none bg-transparent px-2 py-1.5 text-sm outline-none placeholder:text-muted"
        />
        <IconButton label="Enviar nota" onClick={() => void send()} disabled={!draft.trim() || busy} className="bg-primary text-primary-ink hover:bg-primary hover:text-primary-ink hover:brightness-110">
          <SendHorizontal className="size-4" />
        </IconButton>
      </div>
      {messages.length === 0 ? <p className="py-6 text-center text-sm text-muted">Nenhuma nota nesta pasta.</p> : null}
      <div className="columns-1 gap-3 lg:columns-2 2xl:columns-3 [&>*]:mb-3 [&>*]:break-inside-avoid">
        {messages.map((message) => (
          <NoteCard key={message.id} message={message} onToggleItem={(itemId) => onToggleItem(message, itemId)} onDelete={() => onDelete(message)} onOpenFile={onOpenFile} />
        ))}
      </div>
    </div>
  );
}
