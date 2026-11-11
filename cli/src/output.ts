import { formatBytes, type TransferProgress } from '@relay/core';

export class CliError extends Error {}

const interactive = Boolean(process.stderr.isTTY);

export function info(message: string): void {
  process.stderr.write(`${message}\n`);
}

export function printJson(value: unknown): void {
  process.stdout.write(`${JSON.stringify(value, null, 2)}\n`);
}

export function progressReporter(label: string): { update: (progress: TransferProgress) => void; done: (suffix?: string) => void } {
  let last = 0;
  const started = Date.now();
  const render = (progress: TransferProgress, force = false) => {
    const now = Date.now();
    if (!interactive || (!force && now - last < 120)) return;
    last = now;
    const percent = progress.total > 0 ? Math.min(100, Math.floor((progress.done / progress.total) * 100)) : 0;
    const elapsed = Math.max((now - started) / 1000, 0.001);
    const speed = formatBytes(progress.done / elapsed);
    const width = 24;
    const filled = Math.round((percent / 100) * width);
    const bar = `${'█'.repeat(filled)}${'░'.repeat(width - filled)}`;
    process.stderr.write(`\r\x1b[2K${truncate(label, 40)} ${bar} ${String(percent).padStart(3)}%  ${formatBytes(progress.done)}/${formatBytes(progress.total)}  ${speed}/s`);
  };
  return {
    update: (progress) => render(progress),
    done: (suffix = '') => {
      if (interactive) process.stderr.write('\r\x1b[2K');
      info(`✓ ${label}${suffix ? ` ${suffix}` : ''}`);
    },
  };
}

export function truncate(value: string, width: number): string {
  return value.length <= width ? value.padEnd(width) : `${value.slice(0, width - 1)}…`;
}

export function table(rows: string[][]): string {
  if (rows.length === 0) return '';
  const widths = rows[0]!.map((_, column) => Math.max(...rows.map((row) => (row[column] ?? '').length)));
  return rows.map((row) => row.map((cell, column) => (column === row.length - 1 ? cell : cell.padEnd(widths[column] ?? 0))).join('  ')).join('\n');
}

export function formatDate(value: string | null): string {
  if (!value) return '';
  const date = new Date(value);
  return date.toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}
