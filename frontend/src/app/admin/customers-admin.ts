import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { errorMessage, ToastService } from '../core/toast.service';
import { GroceryApiService } from '../grocery-api.service';
import { Customer, CustomerRequest, Order } from '../models';
import { cleanCustomer, CustomerFormComponent, emptyCustomer } from '../shared/customer-form';
import { OrderItemsComponent } from '../shared/order-items';

interface CustomerDialog {
  customerId: number | null;
  model: CustomerRequest;
}

@Component({
  selector: 'app-customers-admin',
  imports: [FormsModule, CurrencyPipe, DatePipe, CustomerFormComponent, OrderItemsComponent],
  template: `
    <div class="page">
      <div class="page-head">
        <div>
          <h1>Customers</h1>
        </div>
        <button class="btn btn-primary" type="button" (click)="openCustomer(null)">New customer</button>
      </div>

      <div class="toolbar">
        <form class="field grow" (ngSubmit)="search.set(searchText.trim())">
          Search
          <input class="input" name="search" type="search" placeholder="Name, city, or business type" [(ngModel)]="searchText">
        </form>
        <div class="field">
          Type
          <div class="segmented">
            <button type="button" [class.active]="type() === ''" (click)="type.set('')">All</button>
            <button type="button" [class.active]="type() === 'HOME'" (click)="type.set('HOME')">Home</button>
            <button type="button" [class.active]="type() === 'BUSINESS'" (click)="type.set('BUSINESS')">Business</button>
          </div>
        </div>
      </div>

      @if (error()) {
        <div class="notice error">{{ error() }}</div>
      }

      <div class="card">
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Customer</th>
                <th>Type</th>
                <th>Details</th>
                <th class="num">Orders</th>
                <th class="num">Total spent</th>
                <th class="num">Last order</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (customer of customers(); track customer.customerId) {
                <tr>
                  <td>
                    {{ customer.name }}
                    <span class="sub">#{{ customer.customerId }} · {{ customer.city || 'No address' }}{{ customer.hasAccount ? ' · has login' : '' }}</span>
                  </td>
                  <td><span class="pill plain">{{ customer.customerType === 'HOME' ? 'Home' : 'Business' }}</span></td>
                  <td class="small">
                    @if (customer.customerType === 'BUSINESS') {
                      {{ customer.businessCategory }}
                      @if (customer.annualIncome !== null) { · {{ customer.annualIncome | currency: 'USD' : 'symbol' : '1.0-0' }}/yr }
                    } @else {
                      {{ customer.age !== null ? 'Age ' + customer.age : 'Age -' }}
                      @if (customer.maritalStatus) { · {{ customer.maritalStatus }} }
                    }
                  </td>
                  <td class="num">{{ customer.orderCount }}</td>
                  <td class="num">{{ customer.totalSpent | currency }}</td>
                  <td class="num">{{ customer.lastOrderDate ? (customer.lastOrderDate | date: 'mediumDate') : '-' }}</td>
                  <td class="num">
                    <button class="btn btn-sm btn-ghost" type="button" (click)="showOrders(customer)">Orders</button>
                    <button class="btn btn-sm btn-ghost" type="button" (click)="openCustomer(customer)">Edit</button>
                    @if (customer.orderCount === 0) {
                      <button class="btn btn-sm btn-danger" type="button" [disabled]="deleting() === customer.customerId" (click)="remove(customer)">Delete</button>
                    }
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="7" class="empty">{{ loading() ? 'Loading customers...' : 'No customers match.' }}</td></tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>

    @if (dialog(); as current) {
      <div class="backdrop" (click)="dialog.set(null)">
        <form class="dialog wide" role="dialog" aria-modal="true" aria-labelledby="customer-title" (click)="$event.stopPropagation()" (ngSubmit)="save(current)">
          <div class="dialog-head">
            <h2 id="customer-title">{{ current.customerId === null ? 'New customer' : 'Edit customer #' + current.customerId }}</h2>
            <button class="icon-btn" type="button" (click)="dialog.set(null)" aria-label="Close">×</button>
          </div>
          <div class="dialog-body">
            <app-customer-form [model]="current.model" />
            @if (dialogError()) {
              <div class="notice error spaced" role="alert">{{ dialogError() }}</div>
            }
            <div class="form-actions">
              <button class="btn" type="button" (click)="dialog.set(null)">Cancel</button>
              <button class="btn btn-primary" type="submit" [disabled]="saving()">{{ saving() ? 'Saving...' : 'Save customer' }}</button>
            </div>
          </div>
        </form>
      </div>
    }

    @if (ordersFor(); as customer) {
      <div class="backdrop" (click)="ordersFor.set(null)">
        <div class="dialog wide" role="dialog" aria-modal="true" aria-labelledby="orders-title" (click)="$event.stopPropagation()">
          <div class="dialog-head">
            <div>
              <h2 id="orders-title">Orders of {{ customer.name }}</h2>
            </div>
            <button class="icon-btn" type="button" (click)="ordersFor.set(null)" aria-label="Close">×</button>
          </div>
          <div class="dialog-body stack">
            @for (order of orders(); track order.orderId) {
              <details class="history">
                <summary>
                  <strong>#{{ order.orderId }}</strong>
                  <span class="muted small">{{ order.orderDate | date: 'mediumDate' }} · #{{ order.storeId }} {{ order.storeCity }} · {{ order.salespersonName || 'no salesperson' }}</span>
                  <span class="pill" [class]="'pill status-' + order.orderStatus">{{ order.orderStatus }}</span>
                  <strong class="history-total">{{ order.total | currency }}</strong>
                </summary>
                <app-order-items [order]="order" />
              </details>
            } @empty {
              <p class="muted">{{ ordersLoading() ? 'Loading...' : 'No orders yet.' }}</p>
            }
          </div>
        </div>
      </div>
    }
  `,
  styles: `
    .spaced { margin-top: 14px; }
    .history { border: 1px solid var(--line); border-radius: 4px; padding: 8px 12px; }
    .history summary { display: flex; flex-wrap: wrap; align-items: center; gap: 6px 12px; cursor: pointer; }
    .history-total { margin-left: auto; }
    .stack { gap: 8px; }
  `
})
export class CustomersAdminComponent {
  private readonly api = inject(GroceryApiService);
  private readonly toast = inject(ToastService);

  readonly customers = signal<Customer[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly search = signal('');
  readonly type = signal('');
  readonly dialog = signal<CustomerDialog | null>(null);
  readonly dialogError = signal('');
  readonly saving = signal(false);
  readonly ordersFor = signal<Customer | null>(null);
  readonly orders = signal<Order[]>([]);
  readonly ordersLoading = signal(false);
  readonly deleting = signal<number | null>(null);
  searchText = '';

  constructor() {
    effect(() => {
      const search = this.search();
      const type = this.type();
      untracked(() => this.load(search, type));
    });
  }

  openCustomer(customer: Customer | null): void {
    this.dialogError.set('');
    this.dialog.set({
      customerId: customer?.customerId ?? null,
      model: customer
        ? {
            name: customer.name,
            customerType: customer.customerType,
            street: customer.street,
            city: customer.city,
            state: customer.state,
            zipCode: customer.zipCode,
            maritalStatus: customer.maritalStatus,
            gender: customer.gender,
            age: customer.age,
            income: customer.income,
            businessCategory: customer.businessCategory,
            annualIncome: customer.annualIncome
          }
        : emptyCustomer()
    });
  }

  save(current: CustomerDialog): void {
    const request = cleanCustomer(current.model);
    if (!request.name) {
      this.dialogError.set('Name is required.');
      return;
    }
    if (request.customerType === 'BUSINESS' && !request.businessCategory) {
      this.dialogError.set('A business customer needs a business category.');
      return;
    }
    this.saving.set(true);
    this.dialogError.set('');
    const call =
      current.customerId === null
        ? this.api.createCustomer(request)
        : this.api.updateCustomer(current.customerId, request);
    call.subscribe({
      next: (customer) => {
        this.saving.set(false);
        this.dialog.set(null);
        this.toast.show(`${customer.name} was saved.`);
        this.load(this.search(), this.type());
      },
      error: (error) => {
        this.saving.set(false);
        this.dialogError.set(errorMessage(error, 'The customer could not be saved.'));
      }
    });
  }

  showOrders(customer: Customer): void {
    this.ordersFor.set(customer);
    this.orders.set([]);
    this.ordersLoading.set(true);
    this.api.getCustomerOrders(customer.customerId).subscribe({
      next: (orders) => {
        this.orders.set(orders);
        this.ordersLoading.set(false);
      },
      error: (error) => {
        this.ordersLoading.set(false);
        this.toast.error(error, 'Orders could not be loaded.');
      }
    });
  }

  remove(customer: Customer): void {
    if (!confirm(`Delete customer ${customer.name} (#${customer.customerId})? This cannot be undone.`)) {
      return;
    }
    this.deleting.set(customer.customerId);
    this.api.deleteCustomer(customer.customerId).subscribe({
      next: () => {
        this.deleting.set(null);
        this.toast.show(`${customer.name} was deleted.`);
        this.load(this.search(), this.type());
      },
      error: (error) => {
        this.deleting.set(null);
        this.toast.error(error, 'The customer could not be deleted.');
      }
    });
  }

  private load(search: string, type: string): void {
    this.loading.set(true);
    this.error.set('');
    this.api.getCustomers(search, type).subscribe({
      next: (customers) => {
        this.customers.set(customers);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(errorMessage(error, 'Customers could not be loaded.'));
        this.loading.set(false);
      }
    });
  }
}
