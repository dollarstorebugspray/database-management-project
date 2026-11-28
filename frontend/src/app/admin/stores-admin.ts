import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CatalogService } from '../core/catalog.service';
import { errorMessage, ToastService } from '../core/toast.service';
import { GroceryApiService } from '../grocery-api.service';
import { Region, RegionRequest, Store, StoreRequest } from '../models';

@Component({
  selector: 'app-stores-admin',
  imports: [FormsModule],
  template: `
    <div class="page">
      <div class="page-head">
        <div>
          <h1>Stores and regions</h1>
        </div>
      </div>

      <section class="card card-pad block">
        <div class="card-head">
          <h2>Regions</h2>
          <button class="btn btn-primary btn-sm" type="button" (click)="openRegion(null)">New region</button>
        </div>
        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>Region</th><th>Regional manager</th><th class="num">Stores</th><th></th></tr></thead>
            <tbody>
              @for (region of regions(); track region.regionId) {
                <tr>
                  <td>{{ region.regionName }}<span class="sub">#{{ region.regionId }}</span></td>
                  <td>{{ region.regionManager || '-' }}</td>
                  <td class="num">{{ region.storeCount }}</td>
                  <td class="num"><button class="btn btn-sm btn-ghost" type="button" (click)="openRegion(region)">Edit</button></td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>

      <section class="card card-pad block">
        <div class="card-head">
          <h2>Stores</h2>
          <button class="btn btn-primary btn-sm" type="button" (click)="openStore(null)" [disabled]="regions().length === 0">New store</button>
        </div>
        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>Store</th><th>Address</th><th>Manager</th><th>Region</th><th class="num">Salespersons</th><th></th></tr></thead>
            <tbody>
              @for (store of catalog.stores(); track store.storeId) {
                <tr>
                  <td>#{{ store.storeId }} {{ store.city }}</td>
                  <td>{{ store.street }}<span class="sub">{{ store.city }}, {{ store.state }} {{ store.zipCode }}</span></td>
                  <td>{{ store.manager || '-' }}</td>
                  <td>{{ store.regionName }}</td>
                  <td class="num">{{ store.salespersonCount }}</td>
                  <td class="num"><button class="btn btn-sm btn-ghost" type="button" (click)="openStore(store)">Edit</button></td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    </div>

    @if (regionDialog(); as dialog) {
      <div class="backdrop" (click)="regionDialog.set(null)">
        <form class="dialog" role="dialog" aria-modal="true" aria-labelledby="region-title" (click)="$event.stopPropagation()" (ngSubmit)="saveRegion(dialog.id, dialog.model)">
          <div class="dialog-head">
            <h2 id="region-title">{{ dialog.id === null ? 'New region' : 'Edit region' }}</h2>
            <button class="icon-btn" type="button" (click)="regionDialog.set(null)" aria-label="Close">×</button>
          </div>
          <div class="dialog-body">
            <div class="form-grid">
              <label class="field full">Region name<input name="regionName" required maxlength="50" [(ngModel)]="dialog.model.regionName"></label>
              <label class="field full">Regional manager<input name="regionManager" maxlength="50" [(ngModel)]="dialog.model.regionManager"></label>
            </div>
            @if (dialogError()) {
              <div class="notice error spaced" role="alert">{{ dialogError() }}</div>
            }
            <div class="form-actions">
              <button class="btn" type="button" (click)="regionDialog.set(null)">Cancel</button>
              <button class="btn btn-primary" type="submit" [disabled]="saving()">Save region</button>
            </div>
          </div>
        </form>
      </div>
    }

    @if (storeDialog(); as dialog) {
      <div class="backdrop" (click)="storeDialog.set(null)">
        <form class="dialog wide" role="dialog" aria-modal="true" aria-labelledby="store-title" (click)="$event.stopPropagation()" (ngSubmit)="saveStore(dialog.id, dialog.model)">
          <div class="dialog-head">
            <h2 id="store-title">{{ dialog.id === null ? 'New store' : 'Edit store #' + dialog.id }}</h2>
            <button class="icon-btn" type="button" (click)="storeDialog.set(null)" aria-label="Close">×</button>
          </div>
          <div class="dialog-body">
            <div class="form-grid">
              <label class="field full">Street<input name="street" required maxlength="50" [(ngModel)]="dialog.model.street"></label>
              <label class="field">City<input name="city" required maxlength="25" [(ngModel)]="dialog.model.city"></label>
              <label class="field">State<input name="state" required maxlength="15" [(ngModel)]="dialog.model.state"></label>
              <label class="field">ZIP code<input name="zipCode" required maxlength="10" pattern="\\d{5}(-\\d{4})?" [(ngModel)]="dialog.model.zipCode"></label>
              <label class="field">Store manager<input name="manager" maxlength="50" [(ngModel)]="dialog.model.manager"></label>
              <label class="field">Region
                <select name="regionId" [(ngModel)]="dialog.model.regionId">
                  @for (region of regions(); track region.regionId) {
                    <option [ngValue]="region.regionId">{{ region.regionName }}</option>
                  }
                </select>
              </label>
            </div>
            @if (dialogError()) {
              <div class="notice error spaced" role="alert">{{ dialogError() }}</div>
            }
            <div class="form-actions">
              <button class="btn" type="button" (click)="storeDialog.set(null)">Cancel</button>
              <button class="btn btn-primary" type="submit" [disabled]="saving()">Save store</button>
            </div>
          </div>
        </form>
      </div>
    }
  `,
  styles: `
    h2 { font-size: 18px; }
    .block { margin-bottom: 16px; }
    .spaced { margin-top: 14px; }
  `
})
export class StoresAdminComponent {
  private readonly api = inject(GroceryApiService);
  private readonly toast = inject(ToastService);
  readonly catalog = inject(CatalogService);

  readonly regions = signal<Region[]>([]);
  readonly regionDialog = signal<{ id: number | null; model: RegionRequest } | null>(null);
  readonly storeDialog = signal<{ id: number | null; model: StoreRequest } | null>(null);
  readonly dialogError = signal('');
  readonly saving = signal(false);

  constructor() {
    this.loadRegions();
    this.catalog.reloadStores();
  }

  openRegion(region: Region | null): void {
    this.dialogError.set('');
    this.regionDialog.set({
      id: region?.regionId ?? null,
      model: { regionName: region?.regionName ?? '', regionManager: region?.regionManager ?? '' }
    });
  }

  saveRegion(id: number | null, model: RegionRequest): void {
    this.saving.set(true);
    this.dialogError.set('');
    const call = id === null ? this.api.createRegion(model) : this.api.updateRegion(id, model);
    call.subscribe({
      next: (region) => {
        this.saving.set(false);
        this.regionDialog.set(null);
        this.toast.show(`Region ${region.regionName} was saved.`);
        this.loadRegions();
        this.catalog.reloadStores();
      },
      error: (error) => {
        this.saving.set(false);
        this.dialogError.set(errorMessage(error, 'The region could not be saved.'));
      }
    });
  }

  openStore(store: Store | null): void {
    this.dialogError.set('');
    this.storeDialog.set({
      id: store?.storeId ?? null,
      model: {
        street: store?.street ?? '',
        city: store?.city ?? '',
        state: store?.state ?? 'PA',
        zipCode: store?.zipCode ?? '',
        manager: store?.manager ?? '',
        regionId: store?.regionId ?? this.regions()[0]?.regionId ?? 1
      }
    });
  }

  saveStore(id: number | null, model: StoreRequest): void {
    this.saving.set(true);
    this.dialogError.set('');
    const call = id === null ? this.api.createStore(model) : this.api.updateStore(id, model);
    call.subscribe({
      next: (store) => {
        this.saving.set(false);
        this.storeDialog.set(null);
        this.toast.show(`Store #${store.storeId} ${store.city} was saved.`);
        this.catalog.reloadStores();
        this.loadRegions();
      },
      error: (error) => {
        this.saving.set(false);
        this.dialogError.set(errorMessage(error, 'The store could not be saved.'));
      }
    });
  }

  private loadRegions(): void {
    this.api.getRegions().subscribe({
      next: (regions) => this.regions.set(regions),
      error: (error) => this.toast.error(error, 'Regions could not be loaded.')
    });
  }
}
