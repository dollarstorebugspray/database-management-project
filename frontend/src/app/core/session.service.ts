import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { GroceryApiService } from '../grocery-api.service';
import { AuthUser, Customer, RegisterRequest } from '../models';
import { CatalogService } from './catalog.service';
import { NavService, View } from './nav.service';
import { ToastService } from './toast.service';

const CUSTOMER_VIEWS: View[] = ['shop', 'my-orders', 'profile'];
const STAFF_VIEWS: View[] = ['shop', 'dashboard', 'orders', 'inventory', 'customers', 'staff'];
const ADMIN_VIEWS: View[] = [...STAFF_VIEWS, 'stores'];

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly api = inject(GroceryApiService);
  private readonly catalog = inject(CatalogService);
  private readonly nav = inject(NavService);
  private readonly toast = inject(ToastService);

  readonly user = signal<AuthUser | null>(null);
  readonly customer = signal<Customer | null>(null);
  readonly ready = signal(false);
  readonly authDialog = signal<'login' | 'register' | null>(null);

  readonly isStaff = computed(() => {
    const role = this.user()?.role;
    return role === 'EMPLOYEE' || role === 'ADMIN';
  });
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');
  readonly isCustomer = computed(() => this.user()?.role === 'CUSTOMER');
  readonly allowedViews = computed<View[]>(() => {
    if (this.isAdmin()) {
      return ADMIN_VIEWS;
    }
    if (this.isStaff()) {
      return STAFF_VIEWS;
    }
    if (this.isCustomer()) {
      return CUSTOMER_VIEWS;
    }
    return ['shop'];
  });

  init(): void {
    this.api.me().subscribe({
      next: (user) => {
        this.applyUser(user);
        this.ready.set(true);
      },
      error: () => this.ready.set(true)
    });
  }

  login(email: string, password: string): Observable<AuthUser> {
    return this.api.login(email, password).pipe(tap((user) => this.signedIn(user)));
  }

  register(request: RegisterRequest): Observable<AuthUser> {
    return this.api.register(request).pipe(tap((user) => this.signedIn(user)));
  }

  logout(): void {
    this.api.logout().subscribe({
      complete: () => this.clear(),
      error: () => this.clear()
    });
  }

  handleExpired(): void {
    if (this.user()) {
      this.clear();
      this.toast.show('Your session has ended. Please sign in again.', 'info');
      this.authDialog.set('login');
    }
  }

  refreshCustomer(): void {
    const customerId = this.user()?.customerId;
    if (customerId) {
      this.api.getCustomer(customerId).subscribe((customer) => {
        this.customer.set(customer);
        this.user.update((user) => (user ? { ...user, name: customer.name } : user));
      });
    }
  }

  private signedIn(user: AuthUser): void {
    this.applyUser(user);
    this.authDialog.set(null);
    this.toast.show(`Welcome, ${user.name}.`);
    if (this.isStaff() && this.nav.view() === 'shop') {
      this.nav.go('dashboard');
    }
  }

  private applyUser(user: AuthUser): void {
    this.user.set(user);
    this.customer.set(null);
    if (user.customerId) {
      this.refreshCustomer();
    }
    if (user.storeId) {
      this.catalog.setStore(user.storeId);
    }
    if (!this.allowedViews().includes(this.nav.view())) {
      this.nav.go(this.isStaff() ? 'dashboard' : 'shop');
    }
  }

  private clear(): void {
    this.user.set(null);
    this.customer.set(null);
    if (!this.allowedViews().includes(this.nav.view())) {
      this.nav.go('shop');
    }
  }
}
