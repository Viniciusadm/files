export type FileKind = 'image' | 'video' | 'audio' | 'pdf' | 'archive' | 'code' | 'document' | 'link' | 'other';

const MIME_BY_EXTENSION: Record<string, string> = {
  png: 'image/png', jpg: 'image/jpeg', jpeg: 'image/jpeg', gif: 'image/gif', webp: 'image/webp', heic: 'image/heic',
  bmp: 'image/bmp', svg: 'image/svg+xml', avif: 'image/avif',
  mp4: 'video/mp4', mkv: 'video/x-matroska', webm: 'video/webm', mov: 'video/quicktime', avi: 'video/x-msvideo', '3gp': 'video/3gpp',
  mp3: 'audio/mpeg', m4a: 'audio/mp4', ogg: 'audio/ogg', opus: 'audio/opus', wav: 'audio/wav', flac: 'audio/flac', aac: 'audio/aac',
  pdf: 'application/pdf', zip: 'application/zip', rar: 'application/vnd.rar', '7z': 'application/x-7z-compressed',
  tar: 'application/x-tar', gz: 'application/gzip',
  txt: 'text/plain', md: 'text/markdown', csv: 'text/csv', log: 'text/plain', json: 'application/json', xml: 'application/xml',
  html: 'text/html', css: 'text/css', js: 'text/javascript', ts: 'text/plain', kt: 'text/plain', java: 'text/plain',
  py: 'text/x-python', php: 'text/plain', sql: 'text/plain',
  doc: 'application/msword', docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  xls: 'application/vnd.ms-excel', xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  ppt: 'application/vnd.ms-powerpoint', pptx: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  odt: 'application/vnd.oasis.opendocument.text', ods: 'application/vnd.oasis.opendocument.spreadsheet',
  odp: 'application/vnd.oasis.opendocument.presentation', rtf: 'application/rtf', apk: 'application/vnd.android.package-archive',
};

export function extensionOf(fileName: string): string {
  const dot = fileName.lastIndexOf('.');
  return dot > 0 ? fileName.slice(dot + 1).toLowerCase() : '';
}

export function guessMimeType(fileName: string, hint?: string | null): string {
  if (hint && hint !== 'application/octet-stream') return hint;
  return MIME_BY_EXTENSION[extensionOf(fileName)] ?? 'application/octet-stream';
}

export function classifyFile(fileName: string, mimeType?: string | null): FileKind {
  const mime = (mimeType ?? '').toLowerCase();
  const extension = extensionOf(fileName);
  if (mime.startsWith('image/')) return 'image';
  if (mime.startsWith('video/')) return 'video';
  if (mime.startsWith('audio/')) return 'audio';
  if (mime === 'application/pdf' || extension === 'pdf') return 'pdf';
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(extension)) return 'archive';
  if (['js', 'ts', 'kt', 'java', 'py', 'php', 'html', 'css', 'json', 'md', 'xml', 'sql'].includes(extension)) return 'code';
  if (
    ['xls', 'xlsx', 'csv', 'ods', 'ppt', 'pptx', 'odp', 'txt', 'rtf', 'doc', 'docx', 'odt'].includes(extension) ||
    mime.startsWith('text/') ||
    mime.includes('document')
  ) {
    return 'document';
  }
  return 'other';
}

export function normalizeKind(value: string | null | undefined, fileName: string, mimeType?: string | null): FileKind {
  const known: FileKind[] = ['image', 'video', 'audio', 'pdf', 'archive', 'code', 'document', 'link'];
  if (value && (known as string[]).includes(value)) return value as FileKind;
  return classifyFile(fileName, mimeType);
}

export function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const exponent = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  const value = bytes / 1024 ** exponent;
  return `${value.toLocaleString('pt-BR', { maximumFractionDigits: exponent === 0 || value >= 100 ? 0 : 1 })} ${units[exponent]}`;
}
