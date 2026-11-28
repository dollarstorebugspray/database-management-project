import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';

import { CatalogService } from '../core/catalog.service';
import { NavService } from '../core/nav.service';
import { SessionService } from '../core/session.service';
import { errorMessage, ToastService } from '../core/toast.service';
import { GroceryApiService } from '../grocery-api.service';
import { ItemChangeResponse, Order } from '../models';
import { changeSummary, OrderAdjustComponent } from '../shared/order-adjust';
import { OrderItemsComponent } from '../shared/order-items';

@Component({
  selector: 'app-my-orders',
  imports: [CurrencyPipe, DatePipe, OrderItemsComponent, OrderAdjustComponent],
  template: `
    <div class="page">
      <div class="page-head">
        <div>
          <h1>My orders</h1>
        </div>
        <button class="btn" type="button" (click)="nav.go('shop')">Continue shopping</button>
      </div>

      <p class="summary">
        {{ activeOrders().length }} orders, {{ totalSpent() | currency }} spent, {{ openOrders() }} still open
      </p>

      @if (error()) {
        <div class="notice error">{{ error() }}</div>
      }

      @if (loading()) {
        <p class="muted">Loading your orders...</p>
      } @else {
        <div class="stack">
          @for (order of orders(); track order.orderId) {
            <article class="card order">
              <button class="order-head" type="button" (click)="toggle(order.orderId)" [attr.aria-expanded]="expanded() === order.orderId">
                <span class="order-id">
                  <strong>Order #{{ order.orderId }}</strong>
                  <span class="muted small">{{ order.orderDate | date: 'mediumDate' }} · Store #{{ order.storeId }} {{ order.storeCity }}</span>
                </span>
                <span class="pill" [class]="'pill status-' + order.orderStatus">{{ order.orderStatus }}</span>
                <span class="muted small">{{ order.itemCount }} item{{ order.itemCount === 1 ? '' : 's' }}</span>
                <strong class="order-total">{{ order.total | currency }}</strong>
              </button>
              @if (expanded() === order.orderId) {
                <div class="order-body">
                  <p class="small muted">
                    Paid by {{ order.paymentMethod }} · Salesperson {{ order.salespersonName || 'not assigned' }}
                  </p>
                  @if (adjusting() === order.orderId) {
                    <app-order-adjust [order]="order" mode="cancel" (done)="adjusted($event)" (closed)="adjusting.set(null)" />
                  } @else {
                    <app-order-items [order]="order" />
                    @if (order.orderStatus === 'PENDING' || order.orderStatus === 'PAID') {
                      <div class="form-actions">
                        <button class="btn" type="button" (click)="adjusting.set(order.orderId)">Cancel some items</button>
                        <button class="btn btn-danger" type="button" [disabled]="cancelling() === order.orderId" (click)="cancel(order)">
                          {{ cancelling() === order.orderId ? 'Cancelling...' : 'Cancel this order' }}
                        </button>
                      </div>
                    } @else if (order.orderStatus === 'COMPLETED') {
                      <p class="small muted">To return something from this order, bring it to the store within 30 days.</p>
                    }
                  }
                </div>
              }
            </article>
          } @empty {
            <div class="card empty">
              <p>You have not placed any orders yet.</p>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: `
    .summary { margin-bottom: 12px; color: var(--ink-2); }
    .order { overflow: hidden; }
    .order-head {
      display: grid; grid-template-columns: minmax(0, 1fr) auto auto auto; align-items: center; gap: 14px;
      width: 100%; padding: 10px 14px; border: 0; background: none; text-align: left;
    }
    .order-head:hover { background: var(--surface-2); }
    .order-id { display: grid; }
    .order-total { min-width: 80px; text-align: right; }
    .order-body { padding: 0 14px 14px; display: grid; gap: 10px; }
    @media (max-width: 640px) {
      .order-head { grid-template-columns: 1fr auto; }
    }
  `
})
export class MyOrdersComponent implements OnInit {
  private readonly api = inject(GroceryApiService);
  private readonly session = inject(SessionService);
  private readonly catalog = inject(CatalogService);
  private readonly toast = inject(ToastService);
  readonly nav = inject(NavService);

  readonly orders = signal<Order[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly expanded = signal<number | null>(null);
  readonly cancelling = signal<number | null>(null);
  readonly adjusting = signal<number | null>(null);

  readonly activeOrders = computed(() => this.orders().filter((order) => order.orderStatus !== 'CANCELLED'));
  readonly totalSpent = computed(() => this.activeOrders().reduce((sum, order) => sum + Number(order.total), 0));
  readonly openOrders = computed(
    () => this.orders().filter((order) => !['COMPLETED', 'CANCELLED'].includes(order.orderStatus)).length
  );

  ngOnInit(): void {
    this.load();
  }

  toggle(orderId: number): void {
    this.expanded.update((current) => (current === orderId ? null : orderId));
  }

  adjusted(result: ItemChangeResponse): void {
    this.adjusting.set(null);
    this.orders.update((orders) => orders.map((item) => (item.orderId === result.order.orderId ? result.order : item)));
    this.catalog.inventoryChanged();
    this.toast.show(changeSummary(result));
  }

  cancel(order: Order): void {
    if (!confirm(`Cancel order #${order.orderId}? The items go back on the shelf.`)) {
      return;
    }
    this.cancelling.set(order.orderId);
    this.api.cancelOrder(order.orderId).subscribe({
      next: (updated) => {
        this.cancelling.set(null);
        this.orders.update((orders) => orders.map((item) => (item.orderId === updated.orderId ? updated : item)));
        this.catalog.inventoryChanged();
        this.toast.show(`Order #${order.orderId} was cancelled.`);
      },
      error: (error) => {
        this.cancelling.set(null);
        this.toast.error(error, 'The order could not be cancelled.');
        this.load();
      }
    });
  }

  private load(): void {
    const customerId = this.session.user()?.customerId;
    if (!customerId) {
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.api.getCustomerOrders(customerId).subscribe({
      next: (orders) => {
        this.orders.set(orders);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(errorMessage(error, 'Your orders could not be loaded.'));
        this.loading.set(false);
      }
    });
  }
}
