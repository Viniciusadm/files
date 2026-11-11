import { useSyncExternalStore } from 'react';

export class Store<T> {
  private listeners = new Set<() => void>();

  constructor(private state: T) {}

  get = (): T => this.state;

  set(change: Partial<T> | ((state: T) => Partial<T>)): void {
    const patch = typeof change === 'function' ? change(this.state) : change;
    this.state = { ...this.state, ...patch };
    this.listeners.forEach((listener) => listener());
  }

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };
}

export function useStore<T>(store: Store<T>): T {
  return useSyncExternalStore(store.subscribe, store.get, store.get);
}
