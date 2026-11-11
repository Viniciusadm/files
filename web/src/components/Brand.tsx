export function BrandMark({ className = 'size-9' }: { className?: string }) {
  return <img src="./icon.svg" alt="" className={`${className} rounded-[22%]`} />;
}

export function AuthShell({ children, subtitle }: { children: React.ReactNode; subtitle: string }) {
  return (
    <main className="flex min-h-full items-center justify-center px-4 py-10">
      <div className="animate-in w-full max-w-md">
        <div className="mb-8 flex items-center gap-3">
          <BrandMark className="size-11" />
          <div>
            <h1 className="text-xl font-semibold tracking-tight">Relay</h1>
            <p className="text-sm text-muted">{subtitle}</p>
          </div>
        </div>
        <div className="rounded-2xl border border-line bg-surface p-6 shadow-sm">{children}</div>
      </div>
    </main>
  );
}
