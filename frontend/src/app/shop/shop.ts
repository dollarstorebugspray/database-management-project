import { CurrencyPipe } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CartService } from '../core/cart.service';
import { CatalogService } from '../core/catalog.service';
import { SessionService } from '../core/session.service';
import { errorMessage } from '../core/toast.service';
import { GroceryApiService } from '../grocery-api.service';
import { Product, ProductQuery, ProductSort, StoreAvailability } from '../models';

@Component({
  selector: 'app-shop',
  imports: [FormsModule, CurrencyPipe],
  templateUrl: './shop.html',
  styleUrl: './shop.css'
})
export class ShopComponent {
  private readonly api = inject(GroceryApiService);
  readonly catalog = inject(CatalogService);
  readonly cart = inject(CartService);
  readonly session = inject(SessionService);

  readonly products = signal<Product[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  searchText = '';
  minPrice: number | null = null;
  maxPrice: number | null = null;
  readonly search = signal('');
  readonly categoryId = signal<number | null>(null);
  readonly priceRange = signal<{ min: number | null; max: number | null }>({ min: null, max: null });
  readonly inStock = signal(false);
  readonly sort = signal<ProductSort>('name');
  readonly priceError = signal('');

  readonly detail = signal<Product | null>(null);
  readonly broken = signal<ReadonlySet<number>>(new Set());
  readonly availability = signal<StoreAvailability[] | null>(null);

  readonly filtersActive = computed(
    () =>
      this.search() !== '' ||
      this.categoryId() !== null ||
      this.priceRange().min !== null ||
      this.priceRange().max !== null ||
      this.inStock()
  );
  readonly heading = computed(() => {
    const category = this.catalog.categories().find((item) => item.categoryId === this.categoryId());
    if (this.search()) {
      return `Search: ${this.search()}`;
    }
    return category ? category.categoryName : 'Products';
  });

  private requestId = 0;

  constructor() {
    effect(() => {
      const query: ProductQuery = {
        storeId: this.catalog.storeId(),
        search: this.search(),
        categoryId: this.categoryId(),
        minPrice: this.priceRange().min,
        maxPrice: this.priceRange().max,
        inStock: this.inStock(),
        sort: this.sort()
      };
      this.catalog.inventoryVersion();
      untracked(() => this.load(query));
    });
  }

  applySearch(): void {
    this.search.set(this.searchText.trim());
  }

  applyPrice(): void {
    const min = this.minPrice === null || (this.minPrice as unknown) === '' ? null : Number(this.minPrice);
    const max = this.maxPrice === null || (this.maxPrice as unknown) === '' ? null : Number(this.maxPrice);
    if ((min !== null && min < 0) || (max !== null && max < 0)) {
      this.priceError.set('Prices cannot be negative.');
      return;
    }
    if (min !== null && max !== null && min > max) {
      this.priceError.set('The minimum price is higher than the maximum.');
      return;
    }
    this.priceError.set('');
    this.priceRange.set({ min, max });
  }

  clearFilters(): void {
    this.searchText = '';
    this.minPrice = null;
    this.maxPrice = null;
    this.priceError.set('');
    this.search.set('');
    this.categoryId.set(null);
    this.priceRange.set({ min: null, max: null });
    this.inStock.set(false);
  }

  openDetail(product: Product): void {
    this.detail.set(product);
    this.availability.set(null);
    this.api.getAvailability(product.productId).subscribe({
      next: (rows) => this.availability.set(rows),
      error: () => this.availability.set([])
    });
  }

  shopAt(storeId: number): void {
    this.detail.set(null);
    this.catalog.setStore(storeId);
  }

  add(product: Product): void {
    this.cart.add(product);
  }

  hasImage(product: Product): boolean {
    return !!product.imageUrl && !this.broken().has(product.productId);
  }

  markBroken(productId: number): void {
    this.broken.update((ids) => new Set(ids).add(productId));
  }

  stockLabel(product: Product): string {
    if (product.quantity <= 0) {
      return 'Out of stock';
    }
    return product.quantity <= 5 ? `Only ${product.quantity} left` : `${product.quantity} in stock`;
  }

  private load(query: ProductQuery): void {
    const id = ++this.requestId;
    this.loading.set(true);
    this.error.set('');
    this.api.getProducts(query).subscribe({
      next: (products) => {
        if (id !== this.requestId) {
          return;
        }
        this.products.set(products);
        this.cart.syncStock(products);
        this.loading.set(false);
      },
      error: (error) => {
        if (id !== this.requestId) {
          return;
        }
        this.error.set(errorMessage(error, 'Products could not be loaded.'));
        this.loading.set(false);
      }
    });
  }
}
