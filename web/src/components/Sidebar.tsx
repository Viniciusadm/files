import type { Folder } from '@fles/core';
import { ChevronRight, Clock, Folder as FolderIcon, FolderLock, FolderPlus, Lock, LockOpen, Settings, Star, Upload } from 'lucide-react';
import { useMemo, useState } from 'react';
import { BrandMark } from './Brand.tsx';
import { Button, IconButton } from './ui.tsx';

export type View = { kind: 'recent' } | { kind: 'favorites' } | { kind: 'folder'; id: string };

function NavItem({ active, icon, label, onClick, trailing, depth = 0 }: { active: boolean; icon: React.ReactNode; label: string; onClick: () => void; trailing?: React.ReactNode; depth?: number }) {
  return (
    <div className={`group flex items-center rounded-lg transition ${active ? 'bg-primary-soft text-primary' : 'text-ink hover:bg-sunken'}`} style={{ paddingLeft: depth * 14 }}>
      <button onClick={onClick} className="flex min-w-0 flex-1 items-center gap-2.5 px-2.5 py-1.5 text-left text-sm">
        {icon}
        <span className={`truncate ${active ? 'font-semibold' : ''}`}>{label}</span>
      </button>
      {trailing}
    </div>
  );
}

export function Sidebar({
  folders,
  view,
  onNavigate,
  onNewFolder,
  onUpload,
  onSettings,
  secureConfigured,
  secureUnlocked,
  onToggleSecure,
  counts,
}: {
  folders: Folder[];
  view: View;
  onNavigate: (view: View) => void;
  onNewFolder: () => void;
  onUpload: () => void;
  onSettings: () => void;
  secureConfigured: boolean;
  secureUnlocked: boolean;
  onToggleSecure: () => void;
  counts: Map<string, number>;
}) {
  const [collapsed, setCollapsed] = useState<Set<string>>(new Set());
  const children = useMemo(() => {
    const map = new Map<string | null, Folder[]>();
    const ids = new Set(folders.map((folder) => folder.id));
    for (const folder of folders) {
      const parent = folder.parentId && ids.has(folder.parentId) ? folder.parentId : null;
      map.set(parent, [...(map.get(parent) ?? []), folder]);
    }
    for (const list of map.values()) {
      list.sort((a, b) => Number(a.isSecure) - Number(b.isSecure) || (a.name ?? '').localeCompare(b.name ?? '', 'pt-BR'));
    }
    return map;
  }, [folders]);

  const renderTree = (parentId: string | null, depth: number): React.ReactNode =>
    (children.get(parentId) ?? []).map((folder) => {
      const kids = children.get(folder.id) ?? [];
      const open = !collapsed.has(folder.id);
      const locked = folder.isSecure && folder.name === null;
      const Icon = folder.isSecure ? FolderLock : FolderIcon;
      return (
        <div key={folder.id}>
          <NavItem
            depth={depth}
            active={view.kind === 'folder' && view.id === folder.id}
            onClick={() => onNavigate({ kind: 'folder', id: folder.id })}
            icon={<Icon className="size-4 shrink-0" style={{ color: folder.color }} />}
            label={locked ? 'Pasta segura' : folder.name ?? ''}
            trailing={
              <span className="flex items-center pr-1">
                {counts.get(folder.id) ? <span className="px-1 text-xs text-muted tabular-nums">{counts.get(folder.id)}</span> : null}
                {kids.length > 0 ? (
                  <button
                    aria-label={open ? 'Recolher' : 'Expandir'}
                    onClick={() => setCollapsed((current) => {
                      const next = new Set(current);
                      if (next.has(folder.id)) next.delete(folder.id);
                      else next.add(folder.id);
                      return next;
                    })}
                    className="rounded p-0.5 text-muted hover:text-ink"
                  >
                    <ChevronRight className={`size-3.5 transition ${open ? 'rotate-90' : ''}`} />
                  </button>
                ) : null}
              </span>
            }
          />
          {kids.length > 0 && open ? renderTree(folder.id, depth + 1) : null}
        </div>
      );
    });

  return (
    <aside className="flex h-full w-72 flex-col border-r border-line bg-surface">
      <div className="flex items-center gap-2.5 px-4 pt-4 pb-3">
        <BrandMark className="size-8" />
        <span className="text-lg font-semibold tracking-tight">Fles</span>
      </div>
      <div className="px-3 pb-3">
        <Button variant="primary" className="w-full h-10" onClick={onUpload}>
          <Upload className="size-4" /> Enviar arquivos
        </Button>
      </div>
      <nav className="flex-1 space-y-0.5 overflow-y-auto px-3 pb-3">
        <NavItem active={view.kind === 'recent'} onClick={() => onNavigate({ kind: 'recent' })} icon={<Clock className="size-4" />} label="Recentes" />
        <NavItem active={view.kind === 'favorites'} onClick={() => onNavigate({ kind: 'favorites' })} icon={<Star className="size-4" />} label="Favoritos" />
        <div className="flex items-center justify-between px-2.5 pt-4 pb-1">
          <span className="text-xs font-semibold tracking-wide text-muted uppercase">Pastas</span>
          <IconButton label="Nova pasta" onClick={onNewFolder} className="size-7">
            <FolderPlus className="size-4" />
          </IconButton>
        </div>
        {renderTree(null, 0)}
        {folders.length === 0 ? <p className="px-2.5 py-2 text-sm text-muted">Nenhuma pasta ainda.</p> : null}
      </nav>
      <div className="space-y-0.5 border-t border-line p-3">
        {secureConfigured ? (
          <NavItem
            active={false}
            onClick={onToggleSecure}
            icon={secureUnlocked ? <LockOpen className="size-4" /> : <Lock className="size-4" />}
            label={secureUnlocked ? 'Bloquear pastas seguras' : 'Desbloquear pastas seguras'}
          />
        ) : null}
        <NavItem active={false} onClick={onSettings} icon={<Settings className="size-4" />} label="Ajustes" />
      </div>
    </aside>
  );
}
