import { type ApiClient, type FileItem, type RelayKey, type Folder, type RelayService, type Message } from '@relay/core';
import {
  AlertCircle,
  Download,
  FolderInput,
  FolderPlus,
  LayoutGrid,
  List,
  Lock,
  Menu as MenuIcon,
  MoreHorizontal,
  Pencil,
  RefreshCw,
  Search,
  Star,
  Trash2,
  Upload,
  UploadCloud,
  X,
} from 'lucide-react';
import { useCallback, useEffect, useMemo, useRef, useState, type MouseEvent } from 'react';
import type { Library } from '../lib/library.ts';
import type { MediaCache } from '../lib/media.ts';
import { loadPreferences, savePreferences } from '../lib/storage.ts';
import { useStore } from '../lib/store.ts';
import type { Transfers } from '../lib/transfers.ts';
import { ConfirmDialog, MoveDialog, PinDialog, PromptDialog, SettingsDialog } from './Dialogs.tsx';
import { FileCollection, FolderTiles, type FileActions } from './FileBrowser.tsx';
import { isPreviewable } from './fileVisuals.tsx';
import { ContextMenu, type MenuItem } from './Menu.tsx';
import { NotesPanel } from './NotesPanel.tsx';
import { PreviewModal } from './PreviewModal.tsx';
import { Sidebar, type View } from './Sidebar.tsx';
import { TransferPanel } from './TransferPanel.tsx';
import { Button, IconButton, Spinner, toast, toastError } from './ui.tsx';

const TRANSFER_FOLDER_NAME = 'Transferências';

type Dialog =
  | { type: 'newFolder' }
  | { type: 'renameFolder'; folder: Folder }
  | { type: 'deleteFolder'; folder: Folder }
  | { type: 'renameFile'; file: FileItem }
  | { type: 'deleteFiles'; files: FileItem[] }
  | { type: 'moveFiles'; files: FileItem[] }
  | { type: 'deleteMessage'; message: Message }
  | { type: 'pin'; then?: View }
  | { type: 'settings' };

function normalize(value: string): string {
  return value.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
}

function isTyping(target: EventTarget | null): boolean {
  const element = target as HTMLElement | null;
  return Boolean(element && (element.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(element.tagName)));
}

export function Workspace({
  api,
  service,
  library,
  transfers,
  media,
  relayKey,
  email,
  onLogout,
}: {
  api: ApiClient;
  service: RelayService;
  library: Library;
  transfers: Transfers;
  media: MediaCache;
  relayKey: RelayKey;
  email: string | null;
  onLogout: () => void;
}) {
  const state = useStore(library.store);
  const preferences = useMemo(loadPreferences, []);
  const [view, setView] = useState<View>(preferences.lastFolderId ? { kind: 'folder', id: preferences.lastFolderId } : { kind: 'recent' });
  const [tab, setTab] = useState<'files' | 'notes'>('files');
  const [layout, setLayout] = useState(preferences.view);
  const [query, setQuery] = useState('');
  const [selection, setSelection] = useState<Set<string>>(new Set());
  const [dialog, setDialog] = useState<Dialog | null>(null);
  const [menu, setMenu] = useState<{ anchor: DOMRect; items: MenuItem[] } | null>(null);
  const [preview, setPreview] = useState<FileItem | null>(null);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [dragging, setDragging] = useState(false);
  const fileInput = useRef<HTMLInputElement>(null);
  const searchInput = useRef<HTMLInputElement>(null);
  const lastSelected = useRef<string | null>(null);

  useEffect(() => library.start(), [library]);

  const folderById = useMemo(() => new Map(state.folders.map((folder) => [folder.id, folder])), [state.folders]);
  const secureIds = useMemo(() => new Set(state.folders.filter((folder) => folder.isSecure).map((folder) => folder.id)), [state.folders]);
  const folderNames = useMemo(() => new Map(state.folders.map((folder) => [folder.id, folder.name])), [state.folders]);
  const counts = useMemo(() => {
    const map = new Map<string, number>();
    for (const file of state.files) map.set(file.folderId, (map.get(file.folderId) ?? 0) + 1);
    return map;
  }, [state.files]);

  const currentFolder = view.kind === 'folder' ? folderById.get(view.id) ?? null : null;
  const locked = Boolean(currentFolder?.isSecure && !state.secureUnlocked);
  const searching = query.trim().length > 0;

  useEffect(() => {
    if (state.status === 'ready' && view.kind === 'folder' && !folderById.has(view.id)) setView({ kind: 'recent' });
  }, [state.status, view, folderById]);

  useEffect(() => {
    savePreferences({ lastFolderId: view.kind === 'folder' ? view.id : null });
    setSelection(new Set());
    setSidebarOpen(false);
  }, [view]);

  const visibleFiles = useMemo(() => {
    const readable = state.files.filter((file) => file.name !== null);
    if (searching) {
      const wanted = normalize(query.trim());
      return readable.filter((file) => normalize(file.name ?? '').includes(wanted));
    }
    if (view.kind === 'recent') return readable;
    if (view.kind === 'favorites') return readable.filter((file) => file.isFavorite);
    return locked ? [] : state.files.filter((file) => file.folderId === view.id);
  }, [state.files, searching, query, view, locked]);

  const visibleNotes = useMemo(() => {
    const withContent = state.messages.filter((message) => (message.text && message.text.trim()) || message.listItems.length > 0);
    if (searching) {
      const wanted = normalize(query.trim());
      return withContent.filter((message) => normalize([message.text, ...message.listItems.map((item) => item.text)].join(' ')).includes(wanted));
    }
    if (view.kind === 'folder' && !locked) return withContent.filter((message) => message.folderId === view.id);
    if (view.kind === 'favorites') return withContent.filter((message) => message.isFavorite);
    return [];
  }, [state.messages, searching, query, view, locked]);

  const subfolders = useMemo(
    () => (currentFolder && !searching ? state.folders.filter((folder) => folder.parentId === currentFolder.id).sort((a, b) => (a.name ?? '').localeCompare(b.name ?? '', 'pt-BR')) : []),
    [state.folders, currentFolder, searching],
  );

  const selectedFiles = useMemo(() => visibleFiles.filter((file) => selection.has(file.id)), [visibleFiles, selection]);
  const showNotesTab = view.kind === 'folder' && !searching;
  const activeTab = showNotesTab || (searching && visibleNotes.length > 0) ? tab : 'files';

  const resolveUploadTarget = useCallback(async (): Promise<Folder> => {
    if (currentFolder && !locked) return currentFolder;
    const existing = library.store.get().folders.find((folder) => !folder.isSecure && folder.name === TRANSFER_FOLDER_NAME && !folder.parentId);
    if (existing) return existing;
    const created = await service.createFolder(TRANSFER_FOLDER_NAME, { icon: 'Inbox' });
    await library.refresh();
    return created;
  }, [currentFolder, locked, library, service]);

  const upload = useCallback(async (files: File[]) => {
    if (files.length === 0) return;
    try {
      const target = await resolveUploadTarget();
      transfers.upload(target, files);
      if (!currentFolder) toast(`Enviando para "${target.name}"`);
    } catch (error) {
      toastError(error);
    }
  }, [resolveUploadTarget, transfers, currentFolder]);

  const isSecureFile = useCallback((file: FileItem) => secureIds.has(file.folderId), [secureIds]);

  const download = useCallback((file: FileItem) => transfers.download(file, isSecureFile(file)), [transfers, isSecureFile]);

  const openFile = useCallback((file: FileItem) => {
    if (isPreviewable(file.kind, file.mimeType, file.extension)) setPreview(file);
    else download(file);
  }, [download]);

  const toggleSelect = useCallback((file: FileItem, event: MouseEvent) => {
    setSelection((current) => {
      const next = new Set(current);
      if (event.shiftKey && lastSelected.current) {
        const ids = visibleFiles.map((item) => item.id);
        const from = ids.indexOf(lastSelected.current);
        const to = ids.indexOf(file.id);
        if (from >= 0 && to >= 0) {
          for (const id of ids.slice(Math.min(from, to), Math.max(from, to) + 1)) next.add(id);
          return next;
        }
      }
      if (next.has(file.id)) next.delete(file.id);
      else next.add(file.id);
      lastSelected.current = file.id;
      return next;
    });
  }, [visibleFiles]);

  const fileMenu = useCallback((file: FileItem, anchor: DOMRect) => {
    setMenu({
      anchor,
      items: [
        { label: 'Abrir', icon: <Search className="size-4" />, onSelect: () => openFile(file), hidden: !isPreviewable(file.kind, file.mimeType, file.extension) },
        { label: 'Baixar', icon: <Download className="size-4" />, onSelect: () => download(file) },
        { label: 'Renomear', icon: <Pencil className="size-4" />, onSelect: () => setDialog({ type: 'renameFile', file }) },
        { label: 'Mover', icon: <FolderInput className="size-4" />, onSelect: () => setDialog({ type: 'moveFiles', files: [file] }) },
        {
          label: file.isFavorite ? 'Remover dos favoritos' : 'Favoritar',
          icon: <Star className="size-4" />,
          onSelect: () => void service.toggleFileFavorite(file.id).then(() => library.afterMutation(), toastError),
        },
        { label: 'Apagar', icon: <Trash2 className="size-4" />, danger: true, onSelect: () => setDialog({ type: 'deleteFiles', files: [file] }) },
      ],
    });
  }, [openFile, download, service, library]);

  const actions: FileActions = useMemo(() => ({ open: openFile, menu: fileMenu, toggleSelect }), [openFile, fileMenu, toggleSelect]);

  const navigate = useCallback((next: View) => {
    setQuery('');
    setTab('files');
    if (next.kind === 'folder' && library.store.get().folders.find((folder) => folder.id === next.id)?.isSecure && !library.store.get().secureUnlocked) {
      setDialog({ type: 'pin', then: next });
      return;
    }
    setView(next);
  }, [library]);

  useEffect(() => {
    let depth = 0;
    const hasFiles = (event: DragEvent) => Array.from(event.dataTransfer?.types ?? []).includes('Files');
    const enter = (event: DragEvent) => {
      if (!hasFiles(event)) return;
      event.preventDefault();
      depth++;
      setDragging(true);
    };
    const over = (event: DragEvent) => hasFiles(event) && event.preventDefault();
    const leave = (event: DragEvent) => {
      if (!hasFiles(event)) return;
      depth = Math.max(0, depth - 1);
      if (depth === 0) setDragging(false);
    };
    const drop = (event: DragEvent) => {
      if (!hasFiles(event)) return;
      event.preventDefault();
      depth = 0;
      setDragging(false);
      void upload(Array.from(event.dataTransfer?.files ?? []));
    };
    const paste = (event: ClipboardEvent) => {
      if (isTyping(event.target)) return;
      const files = Array.from(event.clipboardData?.files ?? []);
      if (files.length > 0) {
        event.preventDefault();
        void upload(files.map((file, index) => (file.name === 'image.png' ? new File([file], `colado-${new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-')}${index ? `-${index}` : ''}.png`, { type: file.type }) : file)));
      }
    };
    window.addEventListener('dragenter', enter);
    window.addEventListener('dragover', over);
    window.addEventListener('dragleave', leave);
    window.addEventListener('drop', drop);
    window.addEventListener('paste', paste);
    return () => {
      window.removeEventListener('dragenter', enter);
      window.removeEventListener('dragover', over);
      window.removeEventListener('dragleave', leave);
      window.removeEventListener('drop', drop);
      window.removeEventListener('paste', paste);
    };
  }, [upload]);

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (dialog || preview || isTyping(event.target)) return;
      if (event.key === 'Escape') setSelection(new Set());
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'a' && activeTab === 'files') {
        event.preventDefault();
        setSelection(new Set(visibleFiles.map((file) => file.id)));
      }
      if (event.key === 'Delete' && selectedFiles.length > 0) setDialog({ type: 'deleteFiles', files: selectedFiles });
      if (event.key === '/' || ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k')) {
        event.preventDefault();
        searchInput.current?.focus();
      }
    };
    const beforeUnload = (event: BeforeUnloadEvent) => {
      if (transfers.hasActive()) event.preventDefault();
    };
    window.addEventListener('keydown', onKey);
    window.addEventListener('beforeunload', beforeUnload);
    return () => {
      window.removeEventListener('keydown', onKey);
      window.removeEventListener('beforeunload', beforeUnload);
    };
  }, [dialog, preview, activeTab, visibleFiles, selectedFiles, transfers]);

  const folderMenu = (anchor: DOMRect) => {
    if (!currentFolder) return;
    setMenu({
      anchor,
      items: [
        { label: 'Nova subpasta', icon: <FolderPlus className="size-4" />, onSelect: () => setDialog({ type: 'newFolder' }) },
        { label: 'Renomear pasta', icon: <Pencil className="size-4" />, onSelect: () => setDialog({ type: 'renameFolder', folder: currentFolder }) },
        { label: 'Apagar pasta', icon: <Trash2 className="size-4" />, danger: true, onSelect: () => setDialog({ type: 'deleteFolder', folder: currentFolder }) },
      ],
    });
  };

  const breadcrumb = useMemo(() => {
    if (!currentFolder) return [];
    const path: Folder[] = [];
    const seen = new Set<string>();
    let cursor: Folder | undefined = currentFolder;
    while (cursor && !seen.has(cursor.id)) {
      seen.add(cursor.id);
      path.unshift(cursor);
      cursor = cursor.parentId ? folderById.get(cursor.parentId) : undefined;
    }
    return path;
  }, [currentFolder, folderById]);

  const title = searching ? 'Resultados da busca' : view.kind === 'recent' ? 'Recentes' : view.kind === 'favorites' ? 'Favoritos' : null;

  if (state.status === 'loading') {
    return (
      <div className="flex h-full flex-col items-center justify-center gap-3 text-sm text-muted">
        <Spinner className="size-7" />
        Descriptografando seus arquivos…
      </div>
    );
  }

  if (state.status === 'error' && state.folders.length === 0) {
    return (
      <div className="flex h-full flex-col items-center justify-center gap-4 px-6 text-center">
        <AlertCircle className="size-8 text-danger" />
        <p className="max-w-sm text-sm text-muted">{state.error}</p>
        <div className="flex gap-2">
          <Button onClick={() => void library.refresh(true)}>
            <RefreshCw className="size-4" /> Tentar de novo
          </Button>
          <Button variant="ghost" onClick={onLogout}>Sair</Button>
        </div>
      </div>
    );
  }

  const unreadable = state.folders.length > 0 && state.folders.every((folder) => folder.name === null);

  return (
    <div className="flex h-full">
      <div className={`fixed inset-0 z-30 bg-black/30 transition md:hidden ${sidebarOpen ? 'opacity-100' : 'pointer-events-none opacity-0'}`} onClick={() => setSidebarOpen(false)} />
      <div className={`fixed inset-y-0 left-0 z-40 transition-transform md:static md:translate-x-0 ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}`}>
        <Sidebar
          folders={state.folders}
          view={view}
          counts={counts}
          onNavigate={navigate}
          onNewFolder={() => {
            setView((current) => (current.kind === 'folder' ? { kind: 'recent' } : current));
            setDialog({ type: 'newFolder' });
          }}
          onUpload={() => fileInput.current?.click()}
          onSettings={() => setDialog({ type: 'settings' })}
          secureConfigured={state.secureConfigured}
          secureUnlocked={state.secureUnlocked}
          onToggleSecure={() => {
            if (state.secureUnlocked) {
              void library.lockSecure();
              if (currentFolder?.isSecure) setView({ kind: 'recent' });
            } else {
              setDialog({ type: 'pin' });
            }
          }}
        />
      </div>

      <main className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-20 border-b border-line bg-bg/90 backdrop-blur">
          <div className="flex h-16 items-center gap-2 px-3 sm:px-6">
            <IconButton label="Menu" className="md:hidden" onClick={() => setSidebarOpen(true)}>
              <MenuIcon className="size-5" />
            </IconButton>
            {selection.size > 0 ? (
              <div className="flex flex-1 items-center gap-1">
                <IconButton label="Limpar seleção" onClick={() => setSelection(new Set())}>
                  <X className="size-5" />
                </IconButton>
                <span className="mr-auto text-sm font-semibold">{selection.size} selecionado{selection.size > 1 ? 's' : ''}</span>
                <Button variant="ghost" onClick={() => selectedFiles.forEach(download)}>
                  <Download className="size-4" /> <span className="hidden sm:inline">Baixar</span>
                </Button>
                <Button variant="ghost" onClick={() => setDialog({ type: 'moveFiles', files: selectedFiles })}>
                  <FolderInput className="size-4" /> <span className="hidden sm:inline">Mover</span>
                </Button>
                <Button variant="ghost" className="text-danger" onClick={() => setDialog({ type: 'deleteFiles', files: selectedFiles })}>
                  <Trash2 className="size-4" /> <span className="hidden sm:inline">Apagar</span>
                </Button>
              </div>
            ) : (
              <>
                <div className="flex min-w-0 flex-1 items-center gap-1">
                  {title ? (
                    <h1 className="truncate text-lg font-semibold">{title}</h1>
                  ) : (
                    <nav className="flex min-w-0 items-center gap-1 text-lg font-semibold">
                      {breadcrumb.map((folder, index) => (
                        <span key={folder.id} className="flex min-w-0 items-center gap-1">
                          {index > 0 ? <span className="text-muted">/</span> : null}
                          <button
                            onClick={() => navigate({ kind: 'folder', id: folder.id })}
                            className={`truncate rounded-md px-1 hover:bg-sunken ${index < breadcrumb.length - 1 ? 'hidden text-muted sm:inline' : ''}`}
                          >
                            {folder.name ?? 'Pasta segura'}
                          </button>
                        </span>
                      ))}
                      {currentFolder && !locked ? (
                        <IconButton label="Opções da pasta" className="size-8" onClick={(event) => folderMenu(event.currentTarget.getBoundingClientRect())}>
                          <MoreHorizontal className="size-4" />
                        </IconButton>
                      ) : null}
                    </nav>
                  )}
                </div>
                <label className="relative hidden w-64 sm:block lg:w-80">
                  <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted" />
                  <input
                    ref={searchInput}
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                    onKeyDown={(event) => event.key === 'Escape' && (setQuery(''), event.currentTarget.blur())}
                    placeholder="Buscar arquivos e notas"
                    className="h-9 w-full rounded-lg border border-line bg-surface pr-3 pl-9 text-sm outline-none placeholder:text-muted focus:border-primary focus:ring-2 focus:ring-primary/20"
                  />
                </label>
                <IconButton label={layout === 'grid' ? 'Ver em lista' : 'Ver em grade'} onClick={() => {
                  const next = layout === 'grid' ? 'list' : 'grid';
                  setLayout(next);
                  savePreferences({ view: next });
                }}>
                  {layout === 'grid' ? <List className="size-5" /> : <LayoutGrid className="size-5" />}
                </IconButton>
                <span className="hidden sm:block">
                  <Button variant="primary" onClick={() => fileInput.current?.click()} disabled={locked}>
                    <Upload className="size-4" /> Enviar
                  </Button>
                </span>
                <IconButton label="Enviar arquivos" className="sm:hidden" onClick={() => fileInput.current?.click()} disabled={locked}>
                  <Upload className="size-5" />
                </IconButton>
              </>
            )}
          </div>
          <div className="px-3 pb-3 sm:hidden">
            <label className="relative block">
              <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted" />
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Buscar"
                className="h-9 w-full rounded-lg border border-line bg-surface pr-3 pl-9 text-sm outline-none focus:border-primary"
              />
            </label>
          </div>
          {(showNotesTab && !locked) || (searching && visibleNotes.length > 0) ? (
            <div className="flex gap-1 px-3 sm:px-6">
              {(['files', 'notes'] as const).map((id) => (
                <button
                  key={id}
                  onClick={() => setTab(id)}
                  className={`-mb-px border-b-2 px-3 pb-2 text-sm font-medium transition ${activeTab === id ? 'border-primary text-ink' : 'border-transparent text-muted hover:text-ink'}`}
                >
                  {id === 'files' ? `Arquivos (${visibleFiles.length})` : `Notas (${visibleNotes.length})`}
                </button>
              ))}
            </div>
          ) : null}
        </header>

        <div className="flex-1 overflow-y-auto px-3 py-5 sm:px-6">
          {unreadable ? (
            <div className="mb-5 flex items-start gap-3 rounded-xl border border-danger/30 bg-danger-soft p-4 text-sm">
              <AlertCircle className="mt-0.5 size-4 shrink-0 text-danger" />
              <p>Nenhuma pasta pôde ser lida com esta chave. Confira em Ajustes se a impressão digital é a mesma do celular.</p>
            </div>
          ) : null}
          {locked ? (
            <div className="flex flex-col items-center gap-4 py-20 text-center">
              <Lock className="size-10 text-muted" />
              <p className="text-sm text-muted">Esta pasta é segura. Desbloqueie com o PIN do celular.</p>
              <Button variant="primary" onClick={() => setDialog({ type: 'pin' })}>Desbloquear</Button>
            </div>
          ) : activeTab === 'notes' ? (
            <NotesPanel
              messages={visibleNotes}
              disabled={!currentFolder}
              onSend={async (text) => {
                if (!currentFolder) return;
                try {
                  library.upsertLocally(await service.sendNote(currentFolder, text));
                } catch (error) {
                  toastError(error);
                  throw error;
                }
              }}
              onToggleItem={(message, itemId) => {
                library.upsertLocally({ ...message, listItems: message.listItems.map((item) => (item.id === itemId ? { ...item, checked: !item.checked } : item)) });
                void service.toggleListItem(message.id, itemId).then(() => library.afterMutation(), toastError);
              }}
              onDelete={(message) => setDialog({ type: 'deleteMessage', message })}
              onOpenFile={openFile}
            />
          ) : (
            <>
              <FolderTiles folders={subfolders} counts={counts} onOpen={(folder) => navigate({ kind: 'folder', id: folder.id })} />
              {visibleFiles.length === 0 ? (
                <EmptyState
                  searching={searching}
                  favorites={view.kind === 'favorites'}
                  onUpload={() => fileInput.current?.click()}
                  hasNotes={showNotesTab && visibleNotes.length > 0}
                  onShowNotes={() => setTab('notes')}
                />
              ) : (
                <FileCollection
                  files={visibleFiles}
                  layout={layout}
                  selection={selection}
                  media={media}
                  actions={actions}
                  secureFolderIds={secureIds}
                  folderNames={view.kind === 'folder' && !searching ? undefined : folderNames}
                />
              )}
            </>
          )}
        </div>
      </main>

      <input
        ref={fileInput}
        type="file"
        multiple
        hidden
        onChange={(event) => {
          void upload(Array.from(event.target.files ?? []));
          event.target.value = '';
        }}
      />

      {dragging ? (
        <div className="pointer-events-none fixed inset-0 z-50 flex items-center justify-center bg-primary/15 p-6 backdrop-blur-[2px]">
          <div className="flex flex-col items-center gap-3 rounded-3xl border-2 border-dashed border-primary bg-surface/95 px-12 py-10 text-center shadow-2xl">
            <UploadCloud className="size-12 text-primary" />
            <p className="text-lg font-semibold">Solte para enviar</p>
            <p className="text-sm text-muted">para {currentFolder && !locked ? `"${currentFolder.name}"` : `"${TRANSFER_FOLDER_NAME}"`}</p>
          </div>
        </div>
      ) : null}

      {menu ? <ContextMenu anchor={menu.anchor} items={menu.items} onClose={() => setMenu(null)} /> : null}

      {preview ? (
        <PreviewModal
          file={preview}
          siblings={visibleFiles.filter((file) => isPreviewable(file.kind, file.mimeType, file.extension))}
          isSecure={isSecureFile(preview)}
          media={media}
          onClose={() => setPreview(null)}
          onNavigate={setPreview}
          onDownload={download}
        />
      ) : null}

      <TransferPanel transfers={transfers} />

      {dialog?.type === 'newFolder' ? (
        <PromptDialog
          title={currentFolder ? `Nova pasta em "${currentFolder.name}"` : 'Nova pasta'}
          label="Nome"
          confirm="Criar"
          onClose={() => setDialog(null)}
          onSubmit={async (name) => {
            const folder = await service.createFolder(name, { parentId: currentFolder?.id ?? null, isSecure: currentFolder?.isSecure ?? false });
            await library.refresh();
            setView({ kind: 'folder', id: folder.id });
          }}
        />
      ) : null}
      {dialog?.type === 'renameFolder' ? (
        <PromptDialog
          title="Renomear pasta"
          label="Nome"
          initial={dialog.folder.name ?? ''}
          confirm="Salvar"
          onClose={() => setDialog(null)}
          onSubmit={async (name) => {
            await service.renameFolder(dialog.folder, name);
            await library.refresh();
          }}
        />
      ) : null}
      {dialog?.type === 'deleteFolder' ? (
        <ConfirmDialog
          title="Apagar pasta?"
          message={`"${dialog.folder.name}", as subpastas e todos os arquivos dentro delas serão apagados para sempre, inclusive no celular.`}
          confirm="Apagar pasta"
          onClose={() => setDialog(null)}
          onConfirm={async () => {
            await service.deleteFolder(dialog.folder.id);
            setView({ kind: 'recent' });
            await library.refresh(true);
          }}
        />
      ) : null}
      {dialog?.type === 'renameFile' ? (
        <PromptDialog
          title="Renomear arquivo"
          label="Nome"
          initial={dialog.file.name ?? ''}
          confirm="Salvar"
          onClose={() => setDialog(null)}
          onSubmit={async (name) => {
            await service.renameFile(dialog.file, isSecureFile(dialog.file), name);
            await library.refresh();
          }}
        />
      ) : null}
      {dialog?.type === 'deleteFiles' ? (
        <ConfirmDialog
          title={dialog.files.length > 1 ? `Apagar ${dialog.files.length} arquivos?` : 'Apagar arquivo?'}
          message={dialog.files.length > 1 ? 'Os arquivos selecionados serão apagados para sempre, inclusive no celular.' : `"${dialog.files[0]?.name}" será apagado para sempre, inclusive no celular.`}
          confirm="Apagar"
          onClose={() => setDialog(null)}
          onConfirm={async () => {
            const ids = dialog.files.map((file) => file.id);
            await service.deleteFiles(ids);
            library.removeFilesLocally(ids);
            setSelection(new Set());
            void library.afterMutation();
          }}
        />
      ) : null}
      {dialog?.type === 'moveFiles' && dialog.files.length > 0 ? (
        <MoveDialog
          folders={state.folders}
          secure={isSecureFile(dialog.files[0]!)}
          currentFolderId={dialog.files.every((file) => file.folderId === dialog.files[0]!.folderId) ? dialog.files[0]!.folderId : null}
          onClose={() => setDialog(null)}
          onMove={async (target) => {
            if (dialog.files.some((file) => isSecureFile(file) !== target.isSecure)) {
              throw new Error('Não é possível mover entre pastas seguras e comuns.');
            }
            await service.moveFiles(dialog.files.map((file) => file.id), target);
            setSelection(new Set());
            await library.refresh();
            toast(`Movido para "${target.name}"`);
          }}
        />
      ) : null}
      {dialog?.type === 'deleteMessage' ? (
        <ConfirmDialog
          title="Apagar nota?"
          message={dialog.message.attachments.length > 0 ? 'A nota e os arquivos anexados a ela serão apagados.' : 'A nota será apagada para sempre.'}
          confirm="Apagar"
          onClose={() => setDialog(null)}
          onConfirm={async () => {
            await service.deleteMessage(dialog.message.id);
            library.removeMessageLocally(dialog.message.id);
            void library.afterMutation();
          }}
        />
      ) : null}
      {dialog?.type === 'pin' ? (
        <PinDialog
          onClose={() => setDialog(null)}
          onUnlock={async (pin) => {
            await library.unlockSecure(pin);
            if (dialog.then) setView(dialog.then);
          }}
        />
      ) : null}
      {dialog?.type === 'settings' ? <SettingsDialog api={api} relayKey={relayKey} email={email} onLogout={onLogout} onClose={() => setDialog(null)} /> : null}
    </div>
  );
}

function EmptyState({ searching, favorites, onUpload, hasNotes, onShowNotes }: { searching: boolean; favorites: boolean; onUpload: () => void; hasNotes: boolean; onShowNotes: () => void }) {
  if (searching) return <p className="py-16 text-center text-sm text-muted">Nada encontrado.</p>;
  if (favorites) return <p className="py-16 text-center text-sm text-muted">Nenhum favorito ainda. Use o menu de um arquivo para favoritar.</p>;
  return (
    <button onClick={onUpload} className="flex w-full flex-col items-center gap-3 rounded-2xl border-2 border-dashed border-line px-6 py-16 text-center transition hover:border-primary hover:bg-primary-soft/40">
      <UploadCloud className="size-10 text-primary" />
      <span className="text-base font-semibold">Arraste arquivos para cá</span>
      <span className="text-sm text-muted">ou clique para escolher. Também dá para colar com Ctrl+V.</span>
      {hasNotes ? (
        <span
          role="link"
          tabIndex={0}
          onClick={(event) => {
            event.stopPropagation();
            onShowNotes();
          }}
          className="text-sm font-medium text-primary hover:underline"
        >
          Esta pasta tem notas
        </span>
      ) : null}
    </button>
  );
}
