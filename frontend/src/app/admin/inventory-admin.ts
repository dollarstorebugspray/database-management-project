import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CatalogService } from '../core/catalog.service';
import { errorMessage, ToastService } from '../core/toast.service';
import { GroceryApiService } from '../grocery-api.service';
import { CategoryRequest, InventoryItem, ProductWriteRequest, Purchase } from '../models';

interface ProductDialog {
  productId: number | null;
  model: ProductWriteRequest;
}

interface DeliveryDialog {
  item: InventoryItem;
  quantity: number | null;
  unitCost: number | null;
  supplier: string;
}

@Component({
  selector: 'app-inventory-admin',
  imports: [FormsModule, CurrencyPipe, DatePipe],
  template: `
    <div class="page">
      <div class="page-head">
        <div>
          <h1>Inventory</h1>
          <p>Store #{{ catalog.storeId() }} {{ catalog.store()?.city }}</p>
        </div>
        <div class="row">
          <button class="btn" type="button" (click)="openCategory()">New category</button>
          <button class="btn btn-primary" type="button" (click)="openProduct(null)">New product</button>
        </div>
      </div>

      <p class="counts">
        {{ carriedCount() }} products carried,
        <span class="stock-low">{{ lowCount() }} low stock (5 or less)</span>,
        <span class="stock-out">{{ outCount() }} out of stock</span>
      </p>

      <div class="toolbar">
        <div class="field">
          Show
          <div class="segmented">
            <button type="button" [class.active]="view() === 'stock'" (click)="view.set('stock')">Stock levels</button>
            <button type="button" [class.active]="view() === 'deliveries'" (click)="view.set('deliveries')">Deliveries</button>
          </div>
        </div>
        <form class="field grow" (ngSubmit)="search.set(searchText.trim())">
          Search
          <input class="input" name="search" type="search" [placeholder]="view() === 'stock' ? 'Product, brand, or category' : 'Product or supplier'" [(ngModel)]="searchText">
        </form>
        @if (view() === 'stock') {
          <label class="check"><input type="checkbox" [ngModel]="lowOnly()" (ngModelChange)="lowOnly.set($event)"> Low stock only</label>
        }
      </div>

      @if (error()) {
        <div class="notice error">{{ error() }}</div>
      }

      @if (view() === 'stock') {
        <div class="card">
          <div class="table-wrap" [class.refreshing]="loading()">
            <table class="table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Category</th>
                  <th class="num">Price</th>
                  <th class="num">Last cost</th>
                  <th class="num">In stock</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (item of items(); track item.productId) {
                  <tr>
                    <td>
                      <div class="prod">
                        @if (item.imageUrl) {
                          <img class="thumb" [src]="item.imageUrl" alt="">
                        } @else {
                          <span class="thumb letter">{{ item.productName.charAt(0) }}</span>
                        }
                        <div>{{ item.productName }}<span class="sub">{{ item.brand || 'No brand' }} · #{{ item.productId }}</span></div>
                      </div>
                    </td>
                    <td>{{ item.categoryName }}</td>
                    <td class="num">{{ item.price | currency }}</td>
                    <td class="num">{{ item.lastUnitCost === null ? '-' : (item.lastUnitCost | currency) }}</td>
                    <td class="num">
                      @if (!item.carried) {
                        <span class="pill plain">Not carried</span>
                      } @else if (item.quantity === 0) {
                        <span class="pill status-CANCELLED">Out</span>
                      } @else if (item.lowStock) {
                        <span class="pill status-PENDING">{{ item.quantity }} low</span>
                      } @else {
                        {{ item.quantity }}
                      }
                    </td>
                    <td class="num actions">
                      <button class="btn btn-sm" type="button" (click)="openDelivery(item)">{{ item.carried ? 'Receive delivery' : 'Start stocking' }}</button>
                      <button class="btn btn-sm btn-ghost" type="button" (click)="openProduct(item)">Edit</button>
                    </td>
                  </tr>
                } @empty {
                  <tr><td colspan="6" class="empty">{{ loading() ? 'Loading inventory...' : 'No products match.' }}</td></tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      } @else {
        <div class="card">
          <div class="table-wrap" [class.refreshing]="loading()">
            <table class="table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Product</th>
                  <th class="num">Qty</th>
                  <th class="num">Unit cost</th>
                  <th class="num">Total cost</th>
                  <th>Supplier</th>
                </tr>
              </thead>
              <tbody>
                @for (row of purchases(); track row.purchaseId) {
                  <tr>
                    <td class="date">{{ row.purchaseDate | date: 'mediumDate' }}</td>
                    <td>{{ row.productName }}<span class="sub">#{{ row.purchaseId }}</span></td>
                    <td class="num">{{ row.quantity }}</td>
                    <td class="num">{{ row.unitCost | currency }}</td>
                    <td class="num">{{ row.totalCost | currency }}</td>
                    <td>{{ row.supplier || '-' }}</td>
                  </tr>
                } @empty {
                  <tr><td colspan="6" class="empty">{{ loading() ? 'Loading deliveries...' : 'No deliveries match.' }}</td></tr>
                }
              </tbody>
            </table>
          </div>
        </div>
        <p class="small muted footnote">Showing the latest 200 deliveries.</p>
      }
    </div>

    @if (deliveryDialog(); as dialog) {
      <div class="backdrop" (click)="deliveryDialog.set(null)">
        <form class="dialog" role="dialog" aria-modal="true" aria-labelledby="delivery-title" (click)="$event.stopPropagation()" (ngSubmit)="saveDelivery(dialog)">
          <div class="dialog-head">
            <div>
              <h2 id="delivery-title">Receive delivery: {{ dialog.item.productName }}</h2>
            </div>
            <button class="icon-btn" type="button" (click)="deliveryDialog.set(null)" aria-label="Close">×</button>
          </div>
          <div class="dialog-body">
            <p class="small muted">
              Sells for {{ dialog.item.price | currency }} ·
              {{ dialog.item.carried ? dialog.item.quantity + ' in stock now' : 'not carried at this store yet' }}
            </p>
            <div class="form-grid spaced">
              <label class="field">Quantity received<input name="deliveryQty" type="number" min="1" max="10000" step="1" required [(ngModel)]="dialog.quantity"></label>
              <label class="field">Unit cost ($)<input name="deliveryCost" type="number" min="0" step="0.01" required [(ngModel)]="dialog.unitCost">
                <span class="hint">What the store paid per unit.</span>
              </label>
              <label class="field full">Supplier<input name="deliverySupplier" maxlength="50" [(ngModel)]="dialog.supplier"></label>
            </div>
            @if (dialogError()) {
              <div class="notice error spaced" role="alert">{{ dialogError() }}</div>
            }
            <div class="form-actions">
              <button class="btn" type="button" (click)="deliveryDialog.set(null)">Cancel</button>
              <button class="btn btn-primary" type="submit" [disabled]="saving()">{{ saving() ? 'Saving...' : 'Add to stock' }}</button>
            </div>
          </div>
        </form>
      </div>
    }

    @if (productDialog(); as dialog) {
      <div class="backdrop" (click)="productDialog.set(null)">
        <form class="dialog" role="dialog" aria-modal="true" aria-labelledby="product-title" (click)="$event.stopPropagation()" (ngSubmit)="saveProduct(dialog)">
          <div class="dialog-head">
            <div>
              <h2 id="product-title">{{ dialog.productId === null ? 'New product' : 'Edit product #' + dialog.productId }}</h2>
            </div>
            <button class="icon-btn" type="button" (click)="productDialog.set(null)" aria-label="Close">×</button>
          </div>
          <div class="dialog-body">
            <div class="form-grid">
              <label class="field full">Product name<input name="productName" required maxlength="100" [(ngModel)]="dialog.model.productName"></label>
              <label class="field">Brand<input name="brand" maxlength="50" [(ngModel)]="dialog.model.brand"></label>
              <label class="field">Category
                <select name="categoryId" [(ngModel)]="dialog.model.categoryId">
                  @for (category of catalog.categories(); track category.categoryId) {
                    <option [ngValue]="category.categoryId">{{ category.categoryName }}</option>
                  }
                </select>
              </label>
              <label class="field">Price ($)<input name="price" type="number" min="0.01" step="0.01" required [(ngModel)]="dialog.model.price"></label>
              @if (dialog.productId === null) {
                <label class="field">Opening stock<input name="quantity" type="number" min="0" step="1" required [(ngModel)]="dialog.model.quantity"></label>
                <label class="field">Unit cost ($)<input name="unitCost" type="number" min="0" step="0.01" [(ngModel)]="dialog.model.unitCost">
                  <span class="hint">What the store paid per unit for the opening stock.</span>
                </label>
                <label class="field">Supplier<input name="supplier" maxlength="50" [(ngModel)]="dialog.model.supplier"></label>
              } @else {
                <label class="field">Shelf count at this store<input name="quantity" type="number" min="0" step="1" required [(ngModel)]="dialog.model.quantity">
                  <span class="hint">Only to correct a miscount. Use "Receive delivery" for new stock so its cost is recorded.</span>
                </label>
              }
            </div>
            @if (dialogError()) {
              <div class="notice error spaced" role="alert">{{ dialogError() }}</div>
            }
            <div class="form-actions">
              <button class="btn" type="button" (click)="productDialog.set(null)">Cancel</button>
              <button class="btn btn-primary" type="submit" [disabled]="saving()">{{ saving() ? 'Saving...' : 'Save product' }}</button>
            </div>
          </div>
        </form>
      </div>
    }

    @if (categoryDialog(); as category) {
      <div class="backdrop" (click)="categoryDialog.set(null)">
        <form class="dialog" role="dialog" aria-modal="true" aria-labelledby="category-title" (click)="$event.stopPropagation()" (ngSubmit)="saveCategory(category)">
          <div class="dialog-head">
            <h2 id="category-title">New category</h2>
            <button class="icon-btn" type="button" (click)="categoryDialog.set(null)" aria-label="Close">×</button>
          </div>
          <div class="dialog-body">
            <div class="form-grid">
              <label class="field full">Name<input name="categoryName" required maxlength="50" [(ngModel)]="category.categoryName"></label>
              <label class="field full">Description<input name="description" maxlength="100" [(ngModel)]="category.description"></label>
            </div>
            @if (dialogError()) {
              <div class="notice error spaced" role="alert">{{ dialogError() }}</div>
            }
            <div class="form-actions">
              <button class="btn" type="button" (click)="categoryDialog.set(null)">Cancel</button>
              <button class="btn btn-primary" type="submit" [disabled]="saving()">Create category</button>
            </div>
          </div>
        </form>
      </div>
    }
  `,
  styles: `
    .counts { margin-bottom: 12px; }
    .actions, .date { white-space: nowrap; }
    .refreshing { opacity: 0.55; }
    .prod { display: flex; align-items: center; gap: 10px; }
    .thumb { flex: 0 0 auto; width: 32px; height: 32px; padding: 2px; border: 1px solid var(--line); border-radius: 3px; }
    .letter { display: grid; place-items: center; color: var(--muted); font-weight: 700; }
    .spaced { margin-top: 14px; }
    .footnote { margin-top: 10px; }
  `
})
export class InventoryAdminComponent {
  private readonly api = inject(GroceryApiService);
  private readonly toast = inject(ToastService);
  readonly catalog = inject(CatalogService);

  readonly view = signal<'stock' | 'deliveries'>('stock');
  readonly items = signal<InventoryItem[]>([]);
  readonly purchases = signal<Purchase[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly search = signal('');
  readonly lowOnly = signal(false);
  readonly saving = signal(false);
  readonly dialogError = signal('');
  readonly productDialog = signal<ProductDialog | null>(null);
  readonly deliveryDialog = signal<DeliveryDialog | null>(null);
  readonly categoryDialog = signal<CategoryRequest | null>(null);
  readonly allItems = signal<InventoryItem[]>([]);
  searchText = '';

  readonly carriedCount = computed(() => this.allItems().filter((item) => item.carried).length);
  readonly lowCount = computed(() => this.allItems().filter((item) => item.lowStock).length);
  readonly outCount = computed(() => this.allItems().filter((item) => item.carried && item.quantity === 0).length);

  constructor() {
    effect(() => {
      const storeId = this.catalog.storeId();
      const search = this.search();
      const lowOnly = this.lowOnly();
      const view = this.view();
      this.catalog.inventoryVersion();
      untracked(() => (view === 'stock' ? this.load(storeId, search, lowOnly) : this.loadPurchases(storeId, search)));
    });
  }

  openDelivery(item: InventoryItem): void {
    this.dialogError.set('');
    this.deliveryDialog.set({
      item,
      quantity: null,
      unitCost: item.lastUnitCost === null ? null : Number(item.lastUnitCost),
      supplier: item.lastSupplier ?? ''
    });
  }

  saveDelivery(dialog: DeliveryDialog): void {
    const quantity = Number(dialog.quantity);
    const unitCost = Number(dialog.unitCost);
    if (!Number.isInteger(quantity) || quantity < 1) {
      this.dialogError.set('Enter a whole number of units received (at least 1).');
      return;
    }
    if (dialog.unitCost === null || (dialog.unitCost as unknown) === '' || !(unitCost >= 0)) {
      this.dialogError.set('Enter the unit cost (0 or more).');
      return;
    }
    this.saving.set(true);
    this.dialogError.set('');
    this.api.restock(this.catalog.storeId(), dialog.item.productId, quantity, unitCost, dialog.supplier).subscribe({
      next: (product) => {
        this.saving.set(false);
        this.deliveryDialog.set(null);
        this.toast.show(`${product.productName}: ${product.quantity} now in stock.`);
        this.catalog.inventoryChanged();
      },
      error: (error) => {
        this.saving.set(false);
        this.dialogError.set(errorMessage(error, 'The delivery could not be saved.'));
      }
    });
  }

  openProduct(item: InventoryItem | null): void {
    this.dialogError.set('');
    this.productDialog.set({
      productId: item?.productId ?? null,
      model: {
        productName: item?.productName ?? '',
        brand: item?.brand ?? '',
        price: item ? Number(item.price) : (null as unknown as number),
        categoryId: item?.categoryId ?? this.catalog.categories()[0]?.categoryId ?? 1,
        storeId: this.catalog.storeId(),
        quantity: item?.quantity ?? 0,
        unitCost: null,
        supplier: null
      }
    });
  }

  saveProduct(dialog: ProductDialog): void {
    const model = { ...dialog.model, storeId: this.catalog.storeId() };
    model.price = Number(model.price);
    model.quantity = Number(model.quantity);
    const cost = model.unitCost as unknown;
    model.unitCost = cost === null || cost === '' ? null : Number(cost);
    if (!model.productName.trim()) {
      this.dialogError.set('Product name is required.');
      return;
    }
    if (!(model.price > 0)) {
      this.dialogError.set('Price must be greater than zero.');
      return;
    }
    if (!Number.isInteger(model.quantity) || model.quantity < 0) {
      this.dialogError.set('Quantity must be a whole number, zero or more.');
      return;
    }
    if (dialog.productId === null && model.quantity > 0 && (model.unitCost === null || model.unitCost < 0)) {
      this.dialogError.set('Enter the unit cost of the opening stock.');
      return;
    }
    this.saving.set(true);
    this.dialogError.set('');
    const request =
      dialog.productId === null ? this.api.createProduct(model) : this.api.updateProduct(dialog.productId, model);
    request.subscribe({
      next: (product) => {
        this.saving.set(false);
        this.productDialog.set(null);
        this.toast.show(`${product.productName} was saved.`);
        this.catalog.inventoryChanged();
      },
      error: (error) => {
        this.saving.set(false);
        this.dialogError.set(errorMessage(error, 'The product could not be saved.'));
      }
    });
  }

  openCategory(): void {
    this.dialogError.set('');
    this.categoryDialog.set({ categoryName: '', description: '' });
  }

  saveCategory(category: CategoryRequest): void {
    if (!category.categoryName.trim()) {
      this.dialogError.set('Category name is required.');
      return;
    }
    this.saving.set(true);
    this.api.createCategory(category).subscribe({
      next: (created) => {
        this.saving.set(false);
        this.categoryDialog.set(null);
        this.catalog.reloadCategories();
        this.toast.show(`Category ${created.categoryName} was created.`);
      },
      error: (error) => {
        this.saving.set(false);
        this.dialogError.set(errorMessage(error, 'The category could not be created.'));
      }
    });
  }

  private load(storeId: number, search: string, lowOnly: boolean): void {
    this.loading.set(true);
    this.error.set('');
    this.api.getInventory(storeId, search, lowOnly).subscribe({
      next: (items) => {
        this.items.set(items);
        if (!search && !lowOnly) {
          this.allItems.set(items);
        } else {
          this.api.getInventory(storeId, '', false).subscribe((all) => this.allItems.set(all));
        }
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(errorMessage(error, 'Inventory could not be loaded.'));
        this.loading.set(false);
      }
    });
  }

  private loadPurchases(storeId: number, search: string): void {
    this.loading.set(true);
    this.error.set('');
    this.api.getInventory(storeId, '', false).subscribe((all) => this.allItems.set(all));
    this.api.getPurchases(storeId, search).subscribe({
      next: (rows) => {
        this.purchases.set(rows);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(errorMessage(error, 'Deliveries could not be loaded.'));
        this.loading.set(false);
      }
    });
  }
}
