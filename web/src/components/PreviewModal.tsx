import { formatBytes, type FileItem } from '@fles/core';
import { ChevronLeft, ChevronRight, Download, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import type { MediaCache } from '../lib/media.ts';
import { isPreviewable, kindVisuals } from './fileVisuals.tsx';
import { Button, IconButton, Spinner } from './ui.tsx';

const PREVIEW_LIMIT = 512 * 1024 * 1024;

function TextPreview({ url }: { url: string }) {
  const [text, setText] = useState<string | null>(null);
  useEffect(() => {
    let alive = true;
    void fetch(url).then((response) => response.text()).then((value) => alive && setText(value.length > 2_000_000 ? `${value.slice(0, 2_000_000)}\n…` : value));
    return () => {
      alive = false;
    };
  }, [url]);
  if (text === null) return <Spinner />;
  return <pre className="max-h-full w-full max-w-4xl overflow-auto rounded-xl bg-surface p-5 text-left font-mono text-sm leading-relaxed whitespace-pre-wrap text-ink">{text}</pre>;
}

export function PreviewModal({
  file,
  siblings,
  isSecure,
  media,
  onClose,
  onNavigate,
  onDownload,
}: {
  file: FileItem;
  siblings: FileItem[];
  isSecure: boolean;
  media: MediaCache;
  onClose: () => void;
  onNavigate: (file: FileItem) => void;
  onDownload: (file: FileItem) => void;
}) {
  const [url, setUrl] = useState<string | null>(null);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const tooLarge = file.size > PREVIEW_LIMIT;
  const previewable = isPreviewable(file.kind, file.mimeType, file.extension) && !tooLarge;
  const index = siblings.findIndex((item) => item.id === file.id);
  const previous = index > 0 ? siblings[index - 1] : undefined;
  const next = index >= 0 && index < siblings.length - 1 ? siblings[index + 1] : undefined;

  useEffect(() => {
    setUrl(null);
    setError(null);
    setProgress(0);
    if (!previewable) return;
    const controller = new AbortController();
    media
      .load(file, isSecure, { priority: true, signal: controller.signal, onProgress: (done, total) => setProgress(total ? done / total : 0) })
      .then(setUrl, (caught: unknown) => !controller.signal.aborted && setError(caught instanceof Error ? caught.message : String(caught)));
    return () => controller.abort();
  }, [file, isSecure, media, previewable]);

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
      if (event.key === 'ArrowLeft' && previous) onNavigate(previous);
      if (event.key === 'ArrowRight' && next) onNavigate(next);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose, onNavigate, previous, next]);

  const visual = kindVisuals[file.kind];
  const Icon = visual.icon;

  return (
    <div className="fixed inset-0 z-50 flex flex-col bg-black/90 text-white" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <header className="flex items-center gap-3 px-4 py-3">
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium">{file.name ?? 'Arquivo'}</p>
          <p className="text-xs text-white/60">{formatBytes(file.size)}{siblings.length > 1 && index >= 0 ? ` · ${index + 1} de ${siblings.length}` : ''}</p>
        </div>
        <IconButton label="Baixar" className="text-white/80 hover:bg-white/10 hover:text-white" onClick={() => onDownload(file)}>
          <Download className="size-5" />
        </IconButton>
        <IconButton label="Fechar" className="text-white/80 hover:bg-white/10 hover:text-white" onClick={onClose}>
          <X className="size-5" />
        </IconButton>
      </header>
      <div className="relative flex min-h-0 flex-1 items-center justify-center px-4 pb-6 sm:px-16" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
        {previous ? (
          <button aria-label="Anterior" onClick={() => onNavigate(previous)} className="absolute left-2 z-10 rounded-full bg-white/10 p-2 hover:bg-white/20 sm:left-4">
            <ChevronLeft className="size-6" />
          </button>
        ) : null}
        {next ? (
          <button aria-label="Próximo" onClick={() => onNavigate(next)} className="absolute right-2 z-10 rounded-full bg-white/10 p-2 hover:bg-white/20 sm:right-4">
            <ChevronRight className="size-6" />
          </button>
        ) : null}
        {!previewable ? (
          <div className="flex flex-col items-center gap-4 text-center">
            <Icon className="size-16 text-white/70" strokeWidth={1.2} />
            <p className="text-sm text-white/70">{tooLarge ? 'Arquivo grande demais para visualizar aqui.' : 'Sem visualização para este tipo de arquivo.'}</p>
            <Button variant="primary" onClick={() => onDownload(file)}>
              <Download className="size-4" /> Baixar
            </Button>
          </div>
        ) : error ? (
          <p className="rounded-lg bg-red-500/20 px-4 py-3 text-sm">{error}</p>
        ) : !url ? (
          <div className="flex w-56 flex-col items-center gap-3">
            <Spinner className="size-7 text-white/70" />
            <div className="h-1 w-full overflow-hidden rounded-full bg-white/15">
              <div className="h-full bg-white/80 transition-[width]" style={{ width: `${Math.round(progress * 100)}%` }} />
            </div>
          </div>
        ) : file.kind === 'image' ? (
          <img src={url} alt={file.name ?? ''} className="max-h-full max-w-full rounded-md object-contain shadow-2xl" />
        ) : file.kind === 'video' ? (
          <video src={url} controls autoPlay className="max-h-full max-w-full rounded-md shadow-2xl" />
        ) : file.kind === 'audio' ? (
          <audio src={url} controls autoPlay className="w-full max-w-lg" />
        ) : file.kind === 'pdf' ? (
          <iframe src={url} title={file.name ?? 'PDF'} className="size-full max-w-5xl rounded-md bg-white" />
        ) : (
          <TextPreview url={url} />
        )}
      </div>
    </div>
  );
}
