import { CurrencyPipe } from '@angular/common';
import { Component, input } from '@angular/core';

import { Order } from '../models';

@Component({
  selector: 'app-order-items',
  imports: [CurrencyPipe],
  template: `
    <table class="table items">
      <thead>
        <tr>
          <th>Product</th>
          <th class="num">Qty</th>
          <th class="num">Unit price</th>
          <th class="num">Line total</th>
        </tr>
      </thead>
      <tbody>
        @for (item of order().items; track item.productId) {
          <tr>
            <td>{{ item.productName }}</td>
            <td class="num">{{ item.quantity }}</td>
            <td class="num">{{ item.unitPrice | currency }}</td>
            <td class="num">{{ item.lineTotal | currency }}</td>
          </tr>
        }
      </tbody>
      <tfoot>
        <tr>
          <td colspan="3" class="num"><strong>Total</strong></td>
          <td class="num"><strong>{{ order().total | currency }}</strong></td>
        </tr>
      </tfoot>
    </table>
  `,
  styles: `
    .items { font-size: 13px; }
    tfoot td { padding: 9px 10px; }
  `
})
export class OrderItemsComponent {
  readonly order = input.required<Order>();
}
