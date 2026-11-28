import { computed, inject, Injectable, signal } from '@angular/core';

import { GroceryApiService } from '../grocery-api.service';
import { Category, Store } from '../models';
import { readStorage, writeStorage } from './storage';
import { ToastService } from './toast.service';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly api = inject(GroceryApiService);
  private readonly toast = inject(ToastService);

  readonly stores = signal<Store[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly storeId = signal<number>(Number(readStorage('gs.storeId')) || 1);
  readonly store = computed(() => this.stores().find((store) => store.storeId === this.storeId()));
  readonly inventoryVersion = signal(0);

  inventoryChanged(): void {
    this.inventoryVersion.update((version) => version + 1);
  }

  load(): void {
    this.api.getStores().subscribe({
      next: (stores) => {
        this.stores.set(stores);
        if (stores.length > 0 && !stores.some((store) => store.storeId === this.storeId())) {
          this.setStore(stores[0].storeId);
        }
      },
      error: (error) => this.toast.error(error, 'Stores could not be loaded.')
    });
    this.reloadCategories();
  }

  reloadStores(): void {
    this.api.getStores().subscribe((stores) => this.stores.set(stores));
  }

  reloadCategories(): void {
    this.api.getCategories().subscribe({
      next: (categories) => this.categories.set(categories),
      error: (error) => this.toast.error(error, 'Categories could not be loaded.')
    });
  }

  setStore(storeId: number): void {
    this.storeId.set(storeId);
    writeStorage('gs.storeId', String(storeId));
  }

  storeLabel(storeId: number | null | undefined): string {
    const store = this.stores().find((item) => item.storeId === storeId);
    return store ? `#${store.storeId} ${store.city} - ${store.street}` : `Store ${storeId ?? ''}`;
  }
}
