import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  text: string;
  kind: 'success' | 'error' | 'info';
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);
  private nextId = 1;

  show(text: string, kind: Toast['kind'] = 'success'): void {
    const toast = { id: this.nextId++, text, kind };
    this.toasts.update((list) => [...list.slice(-3), toast]);
    setTimeout(() => this.dismiss(toast.id), kind === 'error' ? 7000 : 4000);
  }

  error(error: unknown, fallback: string): void {
    this.show(errorMessage(error, fallback), 'error');
  }

  dismiss(id: number): void {
    this.toasts.update((list) => list.filter((toast) => toast.id !== id));
  }
}

export function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Cannot reach the grocery store server. Make sure the backend is running.';
    }
    const body = error.error as { message?: unknown } | null;
    if (body && typeof body === 'object' && typeof body.message === 'string') {
      return body.message;
    }
    return `${fallback} (HTTP ${error.status})`;
  }
  return fallback;
}
