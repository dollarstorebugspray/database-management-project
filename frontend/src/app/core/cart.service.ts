import { computed, Injectable, signal } from '@angular/core';

import { CartItem, Product } from '../models';
import { readStorage, writeStorage } from './storage';

interface SavedCart {
  storeId: number;
  items: CartItem[];
}

@Injectable({ providedIn: 'root' })
export class CartService {
  readonly storeId = signal<number | null>(null);
  readonly items = signal<CartItem[]>([]);
  readonly open = signal(false);

  readonly count = computed(() => this.items().reduce((sum, item) => sum + item.quantity, 0));
  readonly total = computed(() =>
    this.items().reduce((sum, item) => sum + Number(item.product.price) * item.quantity, 0)
  );

  constructor() {
    try {
      const saved = JSON.parse(readStorage('gs.cart') ?? 'null') as SavedCart | null;
      if (saved && Array.isArray(saved.items)) {
        this.storeId.set(saved.storeId);
        this.items.set(saved.items);
      }
    } catch {
    }
  }

  useStore(storeId: number): boolean {
    if (this.storeId() === storeId) {
      return false;
    }
    const hadItems = this.items().length > 0;
    this.storeId.set(storeId);
    this.items.set([]);
    this.save();
    return hadItems;
  }

  quantityOf(productId: number): number {
    return this.items().find((item) => item.product.productId === productId)?.quantity ?? 0;
  }

  add(product: Product, quantity = 1): void {
    const existing = this.items().find((item) => item.product.productId === product.productId);
    const next = Math.min((existing?.quantity ?? 0) + quantity, product.quantity);
    if (next <= 0) {
      return;
    }
    this.items.update((items) =>
      existing
        ? items.map((item) => (item === existing ? { product, quantity: next } : item))
        : [...items, { product, quantity: next }]
    );
    this.save();
  }

  setQuantity(productId: number, quantity: number): void {
    if (quantity < 1) {
      this.remove(productId);
      return;
    }
    this.items.update((items) =>
      items.map((item) =>
        item.product.productId === productId
          ? { ...item, quantity: Math.min(quantity, item.product.quantity) }
          : item
      )
    );
    this.save();
  }

  remove(productId: number): void {
    this.items.update((items) => items.filter((item) => item.product.productId !== productId));
    this.save();
  }

  syncStock(products: Product[]): void {
    const byId = new Map(products.map((product) => [product.productId, product]));
    this.items.update((items) =>
      items
        .map((item) => {
          const fresh = byId.get(item.product.productId);
          return fresh ? { product: fresh, quantity: Math.min(item.quantity, fresh.quantity) } : item;
        })
        .filter((item) => item.quantity > 0)
    );
    this.save();
  }

  clear(): void {
    this.items.set([]);
    this.save();
  }

  private save(): void {
    writeStorage('gs.cart', JSON.stringify({ storeId: this.storeId(), items: this.items() }));
  }
}
