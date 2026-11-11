import { formatBytes, type FileItem, type Folder } from '@relay/core';
import { Check, Folder as FolderIcon, FolderLock, MoreVertical, Star } from 'lucide-react';
import { memo, useEffect, useRef, useState, type MouseEvent } from 'react';
import type { MediaCache } from '../lib/media.ts';
import { formatDate, kindVisuals } from './fileVisuals.tsx';

export interface FileActions {
  open: (file: FileItem) => void;
  menu: (file: FileItem, anchor: DOMRect) => void;
  toggleSelect: (file: FileItem, event: MouseEvent) => void;
}

function useVisible<T extends Element>(): [React.RefObject<T | null>, boolean] {
  const ref = useRef<T>(null);
  const [visible, setVisible] = useState(false);
  useEffect(() => {
    const element = ref.current;
    if (!element || visible) return;
    const observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        setVisible(true);
        observer.disconnect();
      }
    }, { rootMargin: '200px' });
    observer.observe(element);
    return () => observer.disconnect();
  }, [visible]);
  return [ref, visible];
}

function Thumbnail({ file, isSecure, media, large }: { file: FileItem; isSecure: boolean; media: MediaCache; large?: boolean }) {
  const [ref, visible] = useVisible<HTMLDivElement>();
  const [url, setUrl] = useState<string | null>(() => media.cached(file.id));
  const canLoad = media.canThumbnail(file);
  useEffect(() => {
    if (!visible || !canLoad || url) return;
    let alive = true;
    media.load(file, isSecure).then((loaded) => alive && setUrl(loaded), () => undefined);
    return () => {
      alive = false;
    };
  }, [visible, canLoad, url, media, file, isSecure]);
  const visual = kindVisuals[file.kind];
  const Icon = visual.icon;
  return (
    <div ref={ref} className={`flex items-center justify-center overflow-hidden ${large ? 'aspect-[4/3] w-full rounded-t-xl' : 'size-10 shrink-0 rounded-lg'} ${url ? 'bg-sunken' : visual.tint}`}>
      {url ? (
        <img src={url} alt="" className="size-full object-cover" draggable={false} />
      ) : (
        <div className="flex flex-col items-center gap-1">
          <Icon className={large ? 'size-9' : 'size-5'} strokeWidth={1.6} />
          {large && file.extension ? <span className="text-[10px] font-semibold uppercase tracking-wider opacity-70">{file.extension.slice(0, 5)}</span> : null}
        </div>
      )}
    </div>
  );
}

function SelectBox({ selected, onClick }: { selected: boolean; onClick: (event: MouseEvent) => void }) {
  return (
    <button
      aria-label={selected ? 'Desmarcar' : 'Selecionar'}
      onClick={(event) => {
        event.stopPropagation();
        onClick(event);
      }}
      className={`flex size-5 items-center justify-center rounded-md border transition ${selected ? 'border-primary bg-primary text-primary-ink' : 'border-line bg-surface/90 text-transparent hover:border-primary'}`}
    >
      <Check className="size-3.5" strokeWidth={3} />
    </button>
  );
}

const FileCard = memo(function FileCard({ file, isSecure, selected, selecting, media, actions }: { file: FileItem; isSecure: boolean; selected: boolean; selecting: boolean; media: MediaCache; actions: FileActions }) {
  return (
    <div
      role="button"
      tabIndex={0}
      onClick={(event) => (selecting || event.metaKey || event.ctrlKey || event.shiftKey ? actions.toggleSelect(file, event) : actions.open(file))}
      onKeyDown={(event) => event.key === 'Enter' && actions.open(file)}
      onContextMenu={(event) => {
        event.preventDefault();
        actions.menu(file, new DOMRect(event.clientX, event.clientY, 0, 0));
      }}
      className={`group relative flex cursor-pointer flex-col rounded-xl border bg-surface text-left transition hover:shadow-md ${selected ? 'border-primary ring-2 ring-primary/30' : 'border-line'}`}
    >
      <Thumbnail file={file} isSecure={isSecure} media={media} large />
      <div className={`absolute top-2 left-2 transition ${selected || selecting ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'}`}>
        <SelectBox selected={selected} onClick={(event) => actions.toggleSelect(file, event)} />
      </div>
      {file.isFavorite ? <Star className="absolute top-2 right-2 size-4 fill-favorite text-favorite drop-shadow" /> : null}
      <div className="flex items-start gap-1 p-2.5 pr-1">
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium" title={file.name ?? ''}>{file.name ?? 'Nome indisponível'}</p>
          <p className="truncate text-xs text-muted">{formatBytes(file.size)} · {formatDate(file.createdAt)}</p>
        </div>
        <button
          aria-label="Ações"
          onClick={(event) => {
            event.stopPropagation();
            actions.menu(file, event.currentTarget.getBoundingClientRect());
          }}
          className="rounded-md p-1 text-muted opacity-60 hover:bg-sunken hover:text-ink group-hover:opacity-100"
        >
          <MoreVertical className="size-4" />
        </button>
      </div>
    </div>
  );
});

const FileRow = memo(function FileRow({ file, isSecure, selected, selecting, media, actions, folderName }: { file: FileItem; isSecure: boolean; selected: boolean; selecting: boolean; media: MediaCache; actions: FileActions; folderName?: string | null }) {
  return (
    <div
      role="button"
      tabIndex={0}
      onClick={(event) => (selecting || event.metaKey || event.ctrlKey || event.shiftKey ? actions.toggleSelect(file, event) : actions.open(file))}
      onKeyDown={(event) => event.key === 'Enter' && actions.open(file)}
      onContextMenu={(event) => {
        event.preventDefault();
        actions.menu(file, new DOMRect(event.clientX, event.clientY, 0, 0));
      }}
      className={`group flex cursor-pointer items-center gap-3 rounded-lg px-2 py-1.5 transition ${selected ? 'bg-primary-soft' : 'hover:bg-sunken'}`}
    >
      <div className={`transition ${selected || selecting ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'}`}>
        <SelectBox selected={selected} onClick={(event) => actions.toggleSelect(file, event)} />
      </div>
      <Thumbnail file={file} isSecure={isSecure} media={media} />
      <div className="min-w-0 flex-1">
        <p className="flex items-center gap-1.5 truncate text-sm font-medium">
          <span className="truncate">{file.name ?? 'Nome indisponível'}</span>
          {file.isFavorite ? <Star className="size-3.5 shrink-0 fill-favorite text-favorite" /> : null}
        </p>
        {folderName ? <p className="truncate text-xs text-muted">{folderName}</p> : null}
      </div>
      <span className="hidden w-20 text-right text-xs text-muted tabular-nums sm:block">{formatBytes(file.size)}</span>
      <span className="hidden w-24 text-right text-xs text-muted md:block">{formatDate(file.createdAt)}</span>
      <button
        aria-label="Ações"
        onClick={(event) => {
          event.stopPropagation();
          actions.menu(file, event.currentTarget.getBoundingClientRect());
        }}
        className="rounded-md p-1.5 text-muted hover:bg-surface hover:text-ink"
      >
        <MoreVertical className="size-4" />
      </button>
    </div>
  );
});

export function FolderTiles({ folders, onOpen, counts }: { folders: Folder[]; onOpen: (folder: Folder) => void; counts: Map<string, number> }) {
  if (folders.length === 0) return null;
  return (
    <div className="mb-6 grid grid-cols-[repeat(auto-fill,minmax(180px,1fr))] gap-2">
      {folders.map((folder) => {
        const Icon = folder.isSecure ? FolderLock : FolderIcon;
        return (
          <button key={folder.id} onClick={() => onOpen(folder)} className="flex items-center gap-3 rounded-xl border border-line bg-surface px-3 py-2.5 text-left transition hover:bg-sunken">
            <Icon className="size-5 shrink-0" style={{ color: folder.color }} />
            <div className="min-w-0">
              <p className="truncate text-sm font-medium">{folder.name ?? 'Pasta segura'}</p>
              <p className="text-xs text-muted">{counts.get(folder.id) ?? 0} arquivos</p>
            </div>
          </button>
        );
      })}
    </div>
  );
}

export function FileCollection({
  files,
  layout,
  selection,
  media,
  actions,
  secureFolderIds,
  folderNames,
}: {
  files: FileItem[];
  layout: 'grid' | 'list';
  selection: Set<string>;
  media: MediaCache;
  actions: FileActions;
  secureFolderIds: Set<string>;
  folderNames?: Map<string, string | null>;
}) {
  const [limit, setLimit] = useState(240);
  const visible = files.slice(0, limit);
  const selecting = selection.size > 0;
  return (
    <>
      {layout === 'grid' ? (
        <div className="grid grid-cols-[repeat(auto-fill,minmax(150px,1fr))] gap-3">
          {visible.map((file) => (
            <FileCard key={file.id} file={file} isSecure={secureFolderIds.has(file.folderId)} selected={selection.has(file.id)} selecting={selecting} media={media} actions={actions} />
          ))}
        </div>
      ) : (
        <div className="flex flex-col">
          {visible.map((file) => (
            <FileRow key={file.id} file={file} isSecure={secureFolderIds.has(file.folderId)} selected={selection.has(file.id)} selecting={selecting} media={media} actions={actions} folderName={folderNames?.get(file.folderId)} />
          ))}
        </div>
      )}
      {files.length > limit ? (
        <div className="mt-6 flex justify-center">
          <button onClick={() => setLimit((value) => value + 240)} className="rounded-lg px-4 py-2 text-sm font-medium text-primary hover:bg-primary-soft">
            Mostrar mais ({files.length - limit})
          </button>
        </div>
      ) : null}
    </>
  );
}
