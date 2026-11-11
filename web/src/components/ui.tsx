import { Loader2, X } from 'lucide-react';
import { useEffect, useRef, type ButtonHTMLAttributes, type InputHTMLAttributes, type ReactNode } from 'react';
import { Store, useStore } from '../lib/store.ts';

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger';

const variants: Record<Variant, string> = {
  primary: 'bg-primary text-primary-ink hover:brightness-110 shadow-sm',
  secondary: 'bg-surface text-ink border border-line hover:bg-sunken',
  ghost: 'text-ink hover:bg-sunken',
  danger: 'bg-danger text-white hover:brightness-110',
};

export function Button({ variant = 'secondary', busy, className = '', children, ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant; busy?: boolean }) {
  return (
    <button
      {...props}
      disabled={props.disabled || busy}
      className={`inline-flex items-center justify-center gap-2 rounded-lg px-3.5 h-9 text-sm font-medium transition disabled:opacity-50 disabled:pointer-events-none whitespace-nowrap ${variants[variant]} ${className}`}
    >
      {busy ? <Loader2 className="size-4 animate-spin" /> : null}
      {children}
    </button>
  );
}

export function IconButton({ label, className = '', children, ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { label: string }) {
  return (
    <button
      {...props}
      aria-label={label}
      title={label}
      className={`inline-flex size-9 items-center justify-center rounded-lg text-muted transition hover:bg-sunken hover:text-ink disabled:opacity-40 ${className}`}
    >
      {children}
    </button>
  );
}

export function TextField({ label, hint, className = '', ...props }: InputHTMLAttributes<HTMLInputElement> & { label?: string; hint?: ReactNode }) {
  return (
    <label className={`block ${className}`}>
      {label ? <span className="mb-1.5 block text-sm font-medium">{label}</span> : null}
      <input
        {...props}
        className="h-10 w-full rounded-lg border border-line bg-surface px-3 text-sm text-ink outline-none transition placeholder:text-muted focus:border-primary focus:ring-2 focus:ring-primary/20"
      />
      {hint ? <span className="mt-1.5 block text-xs text-muted">{hint}</span> : null}
    </label>
  );
}

export function Spinner({ className = 'size-5' }: { className?: string }) {
  return <Loader2 className={`animate-spin text-muted ${className}`} />;
}

export function Modal({ title, onClose, children, footer, wide }: { title: ReactNode; onClose: () => void; children: ReactNode; footer?: ReactNode; wide?: boolean }) {
  const panel = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKey);
    panel.current?.querySelector<HTMLElement>('input, textarea, button[data-autofocus]')?.focus();
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-0 sm:items-center sm:p-4" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <div ref={panel} role="dialog" aria-modal="true" className={`animate-in flex max-h-[92vh] w-full flex-col rounded-t-2xl bg-surface shadow-2xl sm:rounded-2xl ${wide ? 'sm:max-w-2xl' : 'sm:max-w-md'}`}>
        <div className="flex items-center justify-between gap-4 px-5 pt-4 pb-2">
          <h2 className="text-base font-semibold">{title}</h2>
          <IconButton label="Fechar" onClick={onClose}>
            <X className="size-4" />
          </IconButton>
        </div>
        <div className="overflow-y-auto px-5 pb-5">{children}</div>
        {footer ? <div className="flex justify-end gap-2 border-t border-line px-5 py-3">{footer}</div> : null}
      </div>
    </div>
  );
}

interface Toast {
  id: number;
  message: string;
  tone: 'info' | 'error' | 'success';
}

const toastStore = new Store<{ toasts: Toast[] }>({ toasts: [] });
let toastId = 1;

export function toast(message: string, tone: Toast['tone'] = 'info'): void {
  const id = toastId++;
  toastStore.set((state) => ({ toasts: [...state.toasts, { id, message, tone }] }));
  setTimeout(() => toastStore.set((state) => ({ toasts: state.toasts.filter((item) => item.id !== id) })), tone === 'error' ? 6000 : 3500);
}

export function toastError(error: unknown): void {
  toast(error instanceof Error ? error.message : String(error), 'error');
}

export function Toasts() {
  const { toasts } = useStore(toastStore);
  return (
    <div className="pointer-events-none fixed top-4 sm:top-auto sm:bottom-4 left-1/2 z-[60] flex w-full max-w-sm -translate-x-1/2 flex-col gap-2 px-4">
      {toasts.map((item) => (
        <div
          key={item.id}
          role="status"
          className={`animate-in pointer-events-auto rounded-xl px-4 py-3 text-sm shadow-lg ${item.tone === 'error' ? 'bg-danger text-white' : 'bg-ink text-bg'}`}
        >
          {item.message}
        </div>
      ))}
    </div>
  );
}
