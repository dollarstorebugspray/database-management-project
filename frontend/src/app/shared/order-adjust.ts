import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { errorMessage } from '../core/toast.service';
import { GroceryApiService } from '../grocery-api.service';
import { ItemChangeResponse, Order } from '../models';
import { money } from './format';

@Component({
  selector: 'app-order-adjust',
  imports: [FormsModule, CurrencyPipe],
  template: `
    <div class="adjust">
      <p class="small muted">
        {{ mode() === 'return' ? 'How many of each item is the customer bringing back?' : 'How many of each item should come off the order?' }}
      </p>
      <table class="table">
        <thead>
          <tr>
            <th>Product</th>
            <th class="num">On order</th>
            <th class="num">Unit price</th>
            <th class="num">{{ mode() === 'return' ? 'Return' : 'Cancel' }}</th>
          </tr>
        </thead>
        <tbody>
          @for (item of order().items; track item.productId) {
            <tr>
              <td>{{ item.productName }}</td>
              <td class="num">{{ item.quantity }}</td>
              <td class="num">{{ item.unitPrice | currency }}</td>
              <td class="num">
                <span class="stepper" role="group" [attr.aria-label]="'Amount of ' + item.productName">
                  <button type="button" [disabled]="amount(item.productId) === 0" (click)="change(item.productId, -1, item.quantity)" aria-label="One less">-</button>
                  <span>{{ amount(item.productId) }}</span>
                  <button type="button" [disabled]="amount(item.productId) >= item.quantity" (click)="change(item.productId, 1, item.quantity)" aria-label="One more">+</button>
                </span>
              </td>
            </tr>
          }
        </tbody>
      </table>
      @if (mode() === 'return') {
        <label class="check restock">
          <input type="checkbox" [ngModel]="restock()" (ngModelChange)="restock.set($event)">
          Put the returned items back in stock (untick for damaged or perishable goods)
        </label>
      }
      @if (error()) {
        <div class="notice error" role="alert">{{ error() }}</div>
      }
      <div class="foot">
        <span>
          Refund <strong>{{ refund() | currency }}</strong>
          @if (takesEverything()) {
            <span class="muted small">(the whole order will be cancelled)</span>
          }
        </span>
        <span class="spacer"></span>
        <button class="btn" type="button" (click)="closed.emit()">Back</button>
        <button class="btn btn-primary" type="button" [disabled]="units() === 0 || saving()" (click)="submit()">
          {{ saving() ? 'Saving...' : (mode() === 'return' ? 'Return ' : 'Cancel ') + units() + ' item' + (units() === 1 ? '' : 's') }}
        </button>
      </div>
    </div>
  `,
  styles: `
    .adjust { display: grid; gap: 10px; }
    .stepper { display: inline-flex; align-items: center; border: 1px solid var(--line-strong); border-radius: 4px; }
    .stepper button { width: 28px; height: 28px; border: 0; background: transparent; font-size: 16px; }
    .stepper button:disabled { opacity: 0.35; }
    .stepper span { min-width: 24px; font-weight: 700; text-align: center; }
    .restock { font-size: 13px; }
    .foot { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
  `
})
export class OrderAdjustComponent {
  private readonly api = inject(GroceryApiService);

  readonly order = input.required<Order>();
  readonly mode = input<'cancel' | 'return'>('cancel');
  readonly done = output<ItemChangeResponse>();
  readonly closed = output<void>();

  readonly amounts = signal<Record<number, number>>({});
  readonly restock = signal(true);
  readonly saving = signal(false);
  readonly error = signal('');

  readonly units = computed(() => Object.values(this.amounts()).reduce((sum, value) => sum + value, 0));
  readonly refund = computed(() =>
    this.order().items.reduce((sum, item) => sum + Number(item.unitPrice) * this.amount(item.productId), 0)
  );
  readonly takesEverything = computed(() =>
    this.order().items.every((item) => this.amount(item.productId) >= item.quantity)
  );

  amount(productId: number): number {
    return this.amounts()[productId] ?? 0;
  }

  change(productId: number, step: number, max: number): void {
    const next = Math.min(max, Math.max(0, this.amount(productId) + step));
    this.amounts.update((current) => ({ ...current, [productId]: next }));
  }

  submit(): void {
    const items = Object.entries(this.amounts())
      .filter(([, quantity]) => quantity > 0)
      .map(([productId, quantity]) => ({ productId: Number(productId), quantity }));
    if (items.length === 0) {
      return;
    }
    const verb = this.mode() === 'return' ? 'Return' : 'Cancel';
    const whole = this.takesEverything() ? ' This cancels the whole order.' : '';
    const count = this.units();
    if (!confirm(`${verb} ${count} item${count === 1 ? '' : 's'} from order #${this.order().orderId} and refund ${money(this.refund())}?${whole}`)) {
      return;
    }
    this.saving.set(true);
    this.error.set('');
    const orderId = this.order().orderId;
    const call = this.mode() === 'return'
      ? this.api.returnItems(orderId, items, this.restock())
      : this.api.cancelItems(orderId, items);
    call.subscribe({
      next: (result) => {
        this.saving.set(false);
        this.amounts.set({});
        this.done.emit(result);
      },
      error: (error) => {
        this.saving.set(false);
        this.error.set(errorMessage(error, 'The order could not be changed.'));
      }
    });
  }
}

export function changeSummary(result: ItemChangeResponse): string {
  const orderId = result.order.orderId;
  const refund = money(result.refund);
  if (result.orderCancelled) {
    return `Order #${orderId} was cancelled. Refund ${refund}.`;
  }
  const units = result.unitsRemoved;
  return `${units} item${units === 1 ? '' : 's'} taken off order #${orderId}. Refund ${refund}.`;
}
