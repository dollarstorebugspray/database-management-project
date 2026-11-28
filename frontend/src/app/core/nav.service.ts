import { Injectable, signal } from '@angular/core';

export type View =
  | 'shop'
  | 'my-orders'
  | 'profile'
  | 'dashboard'
  | 'orders'
  | 'inventory'
  | 'customers'
  | 'staff'
  | 'stores';

const VIEWS: View[] = ['shop', 'my-orders', 'profile', 'dashboard', 'orders', 'inventory', 'customers', 'staff', 'stores'];

@Injectable({ providedIn: 'root' })
export class NavService {
  readonly view = signal<View>(this.readHash());

  constructor() {
    window.addEventListener('hashchange', () => this.view.set(this.readHash()));
  }

  go(view: View): void {
    if (this.view() !== view) {
      location.hash = `/${view}`;
    }
    this.view.set(view);
    window.scrollTo({ top: 0 });
  }

  private readHash(): View {
    const value = location.hash.replace(/^#\/?/, '') as View;
    return VIEWS.includes(value) ? value : 'shop';
  }
}
