import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CartService } from '../core/cart.service';
import { CatalogService } from '../core/catalog.service';
import { NavService } from '../core/nav.service';
import { SessionService } from '../core/session.service';
import { errorMessage, ToastService } from '../core/toast.service';
import { GroceryApiService } from '../grocery-api.service';
import { CreateOrderRequest, Customer, PAYMENT_METHODS } from '../models';
import { money } from '../shared/format';

@Component({
  selector: 'app-cart-panel',
  imports: [FormsModule, CurrencyPipe],
  templateUrl: './cart-panel.html',
  styleUrl: './cart-panel.css'
})
export class CartPanelComponent {
  private readonly api = inject(GroceryApiService);
  private readonly toast = inject(ToastService);
  private readonly nav = inject(NavService);
  readonly cart = inject(CartService);
  readonly session = inject(SessionService);
  readonly catalog = inject(CatalogService);

  readonly customers = signal<Customer[]>([]);
  readonly customerId = signal<number | null>(null);
  readonly paymentMethod = signal<string>('Credit Card');
  readonly placing = signal(false);
  readonly error = signal('');

  readonly orderCustomer = computed<Customer | null>(() => {
    if (this.session.isStaff()) {
      return this.customers().find((customer) => customer.customerId === this.customerId()) ?? null;
    }
    return this.session.customer();
  });

  readonly storeHasNoStaff = computed(() => this.catalog.store()?.salespersonCount === 0);

  readonly paymentOptions = computed(() =>
    PAYMENT_METHODS.filter(
      (method) => method !== 'Business Invoice' || this.orderCustomer()?.customerType === 'BUSINESS'
    )
  );

  constructor() {
    effect(() => {
      if (this.cart.open() && this.session.isStaff()) {
        untracked(() => this.loadCustomers());
      }
    });
    effect(() => {
      const options: readonly string[] = this.paymentOptions();
      if (!options.includes(this.paymentMethod())) {
        untracked(() => this.paymentMethod.set(options[0]));
      }
    });
    effect(() => {
      this.cart.count();
      untracked(() => this.error.set(''));
    });
  }

  close(): void {
    this.cart.open.set(false);
  }

  signIn(): void {
    this.session.authDialog.set('login');
  }

  placeOrder(): void {
    const user = this.session.user();
    if (!user) {
      this.signIn();
      return;
    }
    const customerId = this.session.isStaff() ? this.customerId() : user.customerId;
    if (!customerId) {
      this.error.set('Choose the customer for this sale.');
      return;
    }

    const request: CreateOrderRequest = {
      customerId,
      storeId: this.catalog.storeId(),
      paymentMethod: this.paymentMethod(),
      items: this.cart.items().map((item) => ({
        productId: item.product.productId,
        quantity: item.quantity
      }))
    };

    this.placing.set(true);
    this.error.set('');
    this.api.createOrder(request).subscribe({
      next: (order) => {
        this.placing.set(false);
        this.cart.clear();
        this.close();
        this.catalog.inventoryChanged();
        const state = order.orderStatus === 'PAID' ? 'paid' : 'awaiting payment';
        this.toast.show(`Order #${order.orderId} placed for ${money(order.total)} (${state}).`);
        if (this.session.isCustomer()) {
          this.nav.go('my-orders');
        }
      },
      error: (error: unknown) => {
        this.placing.set(false);
        this.error.set(errorMessage(error, 'The order could not be placed.'));
        if (error instanceof HttpErrorResponse && (error.status === 409 || error.status === 404)) {
          this.catalog.inventoryChanged();
        }
      }
    });
  }

  private loadCustomers(): void {
    if (this.customers().length > 0) {
      return;
    }
    this.api.getCustomers().subscribe({
      next: (customers) => this.customers.set(customers),
      error: (error) => this.error.set(errorMessage(error, 'Customers could not be loaded.'))
    });
  }
}
