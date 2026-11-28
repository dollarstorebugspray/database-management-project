import { Component, computed, effect, inject, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CustomersAdminComponent } from './admin/customers-admin';
import { DashboardComponent } from './admin/dashboard';
import { InventoryAdminComponent } from './admin/inventory-admin';
import { OrdersAdminComponent } from './admin/orders-admin';
import { StaffAdminComponent } from './admin/staff-admin';
import { StoresAdminComponent } from './admin/stores-admin';
import { AuthDialogComponent } from './account/auth-dialog';
import { MyOrdersComponent } from './account/my-orders';
import { ProfileComponent } from './account/profile';
import { CartService } from './core/cart.service';
import { CatalogService } from './core/catalog.service';
import { NavService, View } from './core/nav.service';
import { SessionService } from './core/session.service';
import { ToastService } from './core/toast.service';
import { Role } from './models';
import { CartPanelComponent } from './shop/cart-panel';
import { ShopComponent } from './shop/shop';

const VIEW_LABELS: Record<View, string> = {
  shop: 'Shop',
  'my-orders': 'My orders',
  profile: 'Profile',
  dashboard: 'Dashboard',
  orders: 'Orders',
  inventory: 'Inventory',
  customers: 'Customers',
  staff: 'Staff',
  stores: 'Stores & regions'
};

@Component({
  selector: 'app-root',
  imports: [
    FormsModule,
    ShopComponent,
    CartPanelComponent,
    AuthDialogComponent,
    MyOrdersComponent,
    ProfileComponent,
    DashboardComponent,
    OrdersAdminComponent,
    InventoryAdminComponent,
    CustomersAdminComponent,
    StaffAdminComponent,
    StoresAdminComponent
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  readonly session = inject(SessionService);
  readonly catalog = inject(CatalogService);
  readonly cart = inject(CartService);
  readonly nav = inject(NavService);
  readonly toast = inject(ToastService);
  readonly labels = VIEW_LABELS;

  readonly view = computed<View>(() =>
    this.session.allowedViews().includes(this.nav.view()) ? this.nav.view() : 'shop'
  );

  private storeChangedByUser = false;

  constructor() {
    this.session.init();
    this.catalog.load();

    effect(() => {
      const storeId = this.catalog.storeId();
      untracked(() => {
        const dropped = this.cart.useStore(storeId);
        if (dropped && this.storeChangedByUser) {
          this.toast.show('Your cart was emptied because you switched stores.', 'info');
        }
        this.storeChangedByUser = false;
      });
    });
  }

  changeStore(storeId: number): void {
    this.storeChangedByUser = true;
    this.catalog.setStore(storeId);
  }

  goHome(): void {
    this.nav.go(this.session.isStaff() ? 'dashboard' : 'shop');
  }

  roleLabel(role: Role): string {
    return role === 'ADMIN' ? 'Administrator' : role === 'EMPLOYEE' ? 'Salesperson' : 'Customer';
  }
}
