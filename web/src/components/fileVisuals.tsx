import type { FileKind } from '@relay/core';
import { Archive, FileAudio, FileCode, FileImage, FileText, FileVideo, File as FileIcon, Link, type LucideIcon } from 'lucide-react';

export const kindVisuals: Record<FileKind, { icon: LucideIcon; tint: string; label: string }> = {
  image: { icon: FileImage, tint: 'text-sky-500 bg-sky-500/10', label: 'Imagem' },
  video: { icon: FileVideo, tint: 'text-violet-500 bg-violet-500/10', label: 'Vídeo' },
  audio: { icon: FileAudio, tint: 'text-pink-500 bg-pink-500/10', label: 'Áudio' },
  pdf: { icon: FileText, tint: 'text-red-500 bg-red-500/10', label: 'PDF' },
  archive: { icon: Archive, tint: 'text-amber-600 bg-amber-500/10', label: 'Compactado' },
  code: { icon: FileCode, tint: 'text-emerald-600 bg-emerald-500/10', label: 'Código' },
  document: { icon: FileText, tint: 'text-blue-500 bg-blue-500/10', label: 'Documento' },
  link: { icon: Link, tint: 'text-cyan-600 bg-cyan-500/10', label: 'Link' },
  other: { icon: FileIcon, tint: 'text-slate-500 bg-slate-500/10', label: 'Arquivo' },
};

export function formatDate(value: string | null): string {
  if (!value) return '';
  const date = new Date(value);
  const now = new Date();
  const sameDay = date.toDateString() === now.toDateString();
  if (sameDay) return date.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  if (date.toDateString() === yesterday.toDateString()) return 'ontem';
  return date.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: date.getFullYear() === now.getFullYear() ? undefined : 'numeric' });
}

export function isPreviewable(kind: FileKind, mimeType: string, extension: string): boolean {
  if (kind === 'image' || kind === 'video' || kind === 'audio' || kind === 'pdf') return true;
  return mimeType.startsWith('text/') || ['txt', 'md', 'json', 'csv', 'log', 'xml', 'js', 'ts', 'kt', 'py', 'php', 'sql', 'css', 'html'].includes(extension);
}
