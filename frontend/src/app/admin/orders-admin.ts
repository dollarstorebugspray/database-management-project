import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule, NgModel } from '@angular/forms';

import { CatalogService } from '../core/catalog.service';
import { errorMessage, ToastService } from '../core/toast.service';
import { GroceryApiService } from '../grocery-api.service';
import { Salesperson, ItemChangeResponse, Order, ORDER_STATUSES, OrderStatus } from '../models';
import { changeSummary, OrderAdjustComponent } from '../shared/order-adjust';
import { OrderItemsComponent } from '../shared/order-items';

const RETURN_WINDOW_DAYS = 30;
const NEXT: Record<OrderStatus, OrderStatus[]> = {
  PENDING: ['PAID', 'CANCELLED'],
  PAID: ['PROCESSING', 'COMPLETED', 'CANCELLED'],
  PROCESSING: ['COMPLETED', 'CANCELLED'],
  COMPLETED: [],
  CANCELLED: []
};

@Component({
  selector: 'app-orders-admin',
  imports: [FormsModule, CurrencyPipe, DatePipe, OrderItemsComponent, OrderAdjustComponent],
  template: `
    <div class="page">
      <div class="page-head">
        <div>
          <h1>Orders</h1>
        </div>
      </div>

      <div class="toolbar">
        <label class="field">
          Store
          <select [ngModel]="storeId()" (ngModelChange)="storeId.set($event)">
            <option [ngValue]="null">All stores</option>
            @for (store of catalog.stores(); track store.storeId) {
              <option [ngValue]="store.storeId">#{{ store.storeId }} {{ store.city }}</option>
            }
          </select>
        </label>
        <div class="field">
          Status
          <div class="segmented">
            <button type="button" [class.active]="status() === 'ALL'" (click)="status.set('ALL')">All</button>
            @for (item of statuses; track item) {
              <button type="button" [class.active]="status() === item" (click)="status.set(item)">{{ item.charAt(0) + item.slice(1).toLowerCase() }}</button>
            }
          </div>
        </div>
        <form class="field grow" (ngSubmit)="search.set(searchText.trim())">
          Search
          <input class="input" name="search" type="search" placeholder="Order # or customer name" [(ngModel)]="searchText">
        </form>
      </div>

      @if (error()) {
        <div class="notice error">{{ error() }}</div>
      }

      <div class="card">
        <div class="table-wrap" [class.refreshing]="loading()">
          <table class="table">
            <thead>
              <tr>
                <th>Order</th>
                <th>Customer</th>
                <th>Store</th>
                <th>Salesperson</th>
                <th>Payment</th>
                <th class="num">Total</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              @for (order of orders(); track order.orderId) {
                <tr>
                  <td>
                    <button class="link-btn" type="button" (click)="toggle(order.orderId)" [attr.aria-expanded]="expanded() === order.orderId">#{{ order.orderId }}</button>
                    <span class="sub">{{ order.orderDate | date: 'mediumDate' }} · {{ order.itemCount }} item{{ order.itemCount === 1 ? '' : 's' }}</span>
                  </td>
                  <td>{{ order.customerName }}<span class="sub">{{ order.customerType === 'HOME' ? 'Home' : 'Business' }}</span></td>
                  <td>#{{ order.storeId }} {{ order.storeCity }}</td>
                  <td>
                    @if (order.orderStatus === 'CANCELLED') {
                      {{ order.salespersonName || '-' }}
                    } @else {
                      <select class="input compact" #salespersonSelect="ngModel" [ngModel]="order.salespersonId" (ngModelChange)="assign(order, $event, salespersonSelect)" [disabled]="busy() === order.orderId" [attr.aria-label]="'Salesperson for order ' + order.orderId">
                        <option [ngValue]="null" disabled>Unassigned</option>
                        @for (salesperson of salespeopleAt(order.storeId); track salesperson.salespersonId) {
                          <option [ngValue]="salesperson.salespersonId">{{ salesperson.name }}</option>
                        }
                      </select>
                    }
                  </td>
                  <td>{{ order.paymentMethod }}</td>
                  <td class="num">{{ order.total | currency }}</td>
                  <td>
                    @if (nextStatuses(order).length === 0) {
                      <span class="pill" [class]="'pill status-' + order.orderStatus">{{ order.orderStatus }}</span>
                    } @else {
                      <select class="input compact" #statusSelect="ngModel" [ngModel]="order.orderStatus" (ngModelChange)="changeStatus(order, $event, statusSelect)" [disabled]="busy() === order.orderId" [attr.aria-label]="'Status for order ' + order.orderId">
                        <option [ngValue]="order.orderStatus">{{ order.orderStatus }}</option>
                        @for (next of nextStatuses(order); track next) {
                          <option [ngValue]="next">{{ next }}</option>
                        }
                      </select>
                    }
                  </td>
                </tr>
                @if (expanded() === order.orderId) {
                  <tr class="detail-row">
                    <td colspan="7">
                      @if (adjusting() === order.orderId) {
                        <app-order-adjust
                          [order]="order"
                          [mode]="order.orderStatus === 'COMPLETED' ? 'return' : 'cancel'"
                          (done)="adjusted($event)"
                          (closed)="adjusting.set(null)" />
                      } @else {
                        <app-order-items [order]="order" />
                        @if (order.orderStatus === 'PENDING' || order.orderStatus === 'PAID' || order.orderStatus === 'PROCESSING') {
                          <div class="form-actions"><button class="btn" type="button" (click)="adjusting.set(order.orderId)">Cancel items</button></div>
                        } @else if (canReturn(order)) {
                          <div class="form-actions"><button class="btn" type="button" (click)="adjusting.set(order.orderId)">Return items</button></div>
                        } @else if (order.orderStatus === 'COMPLETED') {
                          <p class="small muted">The {{ returnDays }}-day return window for this order has closed.</p>
                        }
                      }
                    </td>
                  </tr>
                }
              } @empty {
                <tr><td colspan="7" class="empty">{{ loading() ? 'Loading orders...' : 'No orders match these filters.' }}</td></tr>
              }
            </tbody>
          </table>
        </div>
      </div>
      <p class="small muted footnote">Showing the latest 200 orders.</p>
    </div>
  `,
  styles: `
    .compact { min-height: 32px; padding: 4px 8px; font-size: 13px; min-width: 140px; }
    .detail-row td { background: var(--surface-2); }
    .refreshing { opacity: 0.55; }
    .footnote { margin-top: 10px; }
  `
})
export class OrdersAdminComponent {
  private readonly api = inject(GroceryApiService);
  private readonly toast = inject(ToastService);
  readonly catalog = inject(CatalogService);

  readonly statuses = ORDER_STATUSES;
  readonly storeId = signal<number | null>(this.catalog.storeId());
  readonly status = signal<string>('ALL');
  readonly search = signal('');
  searchText = '';

  readonly orders = signal<Order[]>([]);
  readonly salespeople = signal<Salesperson[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly expanded = signal<number | null>(null);
  readonly busy = signal<number | null>(null);
  readonly adjusting = signal<number | null>(null);
  readonly returnDays = RETURN_WINDOW_DAYS;

  private readonly salespeopleByStore = computed(() => {
    const map = new Map<number, Salesperson[]>();
    for (const salesperson of this.salespeople()) {
      map.set(salesperson.storeId, [...(map.get(salesperson.storeId) ?? []), salesperson]);
    }
    return map;
  });

  constructor() {
    this.api.getSalespeople(null).subscribe((salespeople) => this.salespeople.set(salespeople));
    effect(() => {
      const filter = { storeId: this.storeId(), status: this.status(), search: this.search() };
      untracked(() => this.load(filter));
    });
  }

  salespeopleAt(storeId: number): Salesperson[] {
    return this.salespeopleByStore().get(storeId) ?? [];
  }

  nextStatuses(order: Order): OrderStatus[] {
    return NEXT[order.orderStatus];
  }

  canReturn(order: Order): boolean {
    if (order.orderStatus !== 'COMPLETED') {
      return false;
    }
    const [year, month, day] = order.orderDate.split('-').map(Number);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const days = Math.round((today.getTime() - new Date(year, month - 1, day).getTime()) / 86400000);
    return days <= RETURN_WINDOW_DAYS;
  }

  adjusted(result: ItemChangeResponse): void {
    this.adjusting.set(null);
    this.replace(result.order);
    this.catalog.inventoryChanged();
    this.toast.show(changeSummary(result));
  }

  toggle(orderId: number): void {
    this.expanded.update((current) => (current === orderId ? null : orderId));
  }

  private revert(select: NgModel, value: unknown): void {
    select.control.setValue(value);
  }

  changeStatus(order: Order, status: OrderStatus, select: NgModel): void {
    if (status === order.orderStatus) {
      return;
    }
    if (status === 'CANCELLED' && !confirm(`Cancel order #${order.orderId}? Its items go back into inventory.`)) {
      this.revert(select, order.orderStatus);
      return;
    }
    this.busy.set(order.orderId);
    this.api.updateOrderStatus(order.orderId, status).subscribe({
      next: (updated) => {
        this.busy.set(null);
        this.replace(updated);
        if (status === 'CANCELLED') {
          this.catalog.inventoryChanged();
        }
        this.toast.show(`Order #${order.orderId} is now ${status.toLowerCase()}.`);
      },
      error: (error) => {
        this.busy.set(null);
        this.revert(select, order.orderStatus);
        this.toast.error(error, 'The status could not be changed.');
      }
    });
  }

  assign(order: Order, salespersonId: number, select: NgModel): void {
    if (salespersonId === order.salespersonId) {
      return;
    }
    this.busy.set(order.orderId);
    this.api.assignSalesperson(order.orderId, salespersonId).subscribe({
      next: (updated) => {
        this.busy.set(null);
        this.replace(updated);
        this.toast.show(`Order #${order.orderId} assigned to ${updated.salespersonName}.`);
      },
      error: (error) => {
        this.busy.set(null);
        this.revert(select, order.salespersonId);
        this.toast.error(error, 'The salesperson could not be changed.');
      }
    });
  }

  private replace(order: Order): void {
    this.orders.update((orders) => orders.map((item) => (item.orderId === order.orderId ? order : item)));
  }

  private load(filter: { storeId: number | null; status: string; search: string }): void {
    this.loading.set(true);
    this.error.set('');
    this.api.getStaffOrders(filter).subscribe({
      next: (orders) => {
        this.orders.set(orders);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(errorMessage(error, 'Orders could not be loaded.'));
        this.loading.set(false);
      }
    });
  }
}
