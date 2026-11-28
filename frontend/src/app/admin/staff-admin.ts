import { CurrencyPipe } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CatalogService } from '../core/catalog.service';
import { SessionService } from '../core/session.service';
import { errorMessage, ToastService } from '../core/toast.service';
import { GroceryApiService } from '../grocery-api.service';
import { Salesperson, SalespersonRequest, StaffAccountRequest } from '../models';

interface SalespersonDialog {
  salespersonId: number | null;
  model: SalespersonRequest;
}

interface LoginDialog {
  salesperson: Salesperson;
  model: StaffAccountRequest;
}

@Component({
  selector: 'app-staff-admin',
  imports: [FormsModule, CurrencyPipe],
  template: `
    <div class="page">
      <div class="page-head">
        <div>
          <h1>Staff</h1>
          @if (!session.isAdmin()) {
            <p>Only administrators can edit staff or see salaries.</p>
          }
        </div>
        @if (session.isAdmin()) {
          <button class="btn btn-primary" type="button" (click)="openSalesperson(null)">Add salesperson</button>
        }
      </div>

      <div class="toolbar">
        <label class="field">
          Store
          <select [ngModel]="storeId()" (ngModelChange)="storeId.set($event)">
            <option [ngValue]="null">All stores</option>
            @for (store of catalog.stores(); track store.storeId) {
              <option [ngValue]="store.storeId">#{{ store.storeId }} {{ store.city }} ({{ store.salespersonCount }} staff)</option>
            }
          </select>
        </label>
        <span class="spacer"></span>
        <span class="muted small">{{ salespeople().length }} salesperson{{ salespeople().length === 1 ? '' : 's' }}</span>
      </div>

      <div class="card">
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Job title</th>
                <th>Store</th>
                <th>Contact</th>
                @if (session.isAdmin()) { <th class="num">Salary</th> }
                <th>Login</th>
                @if (session.isAdmin()) { <th></th> }
              </tr>
            </thead>
            <tbody>
              @for (salesperson of salespeople(); track salesperson.salespersonId) {
                <tr>
                  <td>{{ salesperson.name }}<span class="sub">#{{ salesperson.salespersonId }}</span></td>
                  <td>{{ salesperson.jobTitle }}</td>
                  <td>#{{ salesperson.storeId }} {{ salesperson.storeCity }}</td>
                  <td class="small">
                    {{ salesperson.email || 'No email' }}
                    <span class="sub">{{ address(salesperson) }}</span>
                  </td>
                  @if (session.isAdmin()) {
                    <td class="num">{{ salesperson.salary === null ? '-' : (salesperson.salary | currency: 'USD' : 'symbol' : '1.0-0') }}</td>
                  }
                  <td class="small">
                    @if (salesperson.accountEmail) {
                      {{ salesperson.accountEmail }}
                    } @else if (session.isAdmin()) {
                      <button class="btn btn-sm" type="button" (click)="openLogin(salesperson)">Create login</button>
                    } @else {
                      <span class="muted">None</span>
                    }
                  </td>
                  @if (session.isAdmin()) {
                    <td class="num"><button class="btn btn-sm btn-ghost" type="button" (click)="openSalesperson(salesperson)">Edit</button></td>
                  }
                </tr>
              } @empty {
                <tr><td colspan="7" class="empty">{{ loading() ? 'Loading staff...' : 'No salespeople at this store yet.' }}</td></tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>

    @if (salespersonDialog(); as dialog) {
      <div class="backdrop" (click)="salespersonDialog.set(null)">
        <form class="dialog wide" role="dialog" aria-modal="true" aria-labelledby="salesperson-title" (click)="$event.stopPropagation()" (ngSubmit)="saveSalesperson(dialog)">
          <div class="dialog-head">
            <h2 id="salesperson-title">{{ dialog.salespersonId === null ? 'Add salesperson' : 'Edit salesperson #' + dialog.salespersonId }}</h2>
            <button class="icon-btn" type="button" (click)="salespersonDialog.set(null)" aria-label="Close">×</button>
          </div>
          <div class="dialog-body">
            <div class="form-grid">
              <label class="field">Name<input name="name" required maxlength="50" [(ngModel)]="dialog.model.name"></label>
              <label class="field">Job title
                <input name="jobTitle" required maxlength="50" list="job-titles" [(ngModel)]="dialog.model.jobTitle">
                <datalist id="job-titles">
                  <option value="Cashier"></option>
                  <option value="Sales Associate"></option>
                </datalist>
              </label>
              <label class="field">Store
                <select name="storeId" [(ngModel)]="dialog.model.storeId">
                  @for (store of catalog.stores(); track store.storeId) {
                    <option [ngValue]="store.storeId">#{{ store.storeId }} {{ store.city }}</option>
                  }
                </select>
              </label>
              <label class="field">Salary ($/yr)<input name="salary" type="number" min="0" step="500" [(ngModel)]="dialog.model.salary"></label>
              <label class="field">Email<input name="email" type="email" maxlength="50" [(ngModel)]="dialog.model.email"></label>
              <label class="field full">Street<input name="street" maxlength="50" [(ngModel)]="dialog.model.street"></label>
              <label class="field">City<input name="city" maxlength="25" [(ngModel)]="dialog.model.city"></label>
              <label class="field">State<input name="state" maxlength="15" [(ngModel)]="dialog.model.state"></label>
              <label class="field">ZIP code<input name="zip" maxlength="10" [(ngModel)]="dialog.model.zipCode"></label>
            </div>
            @if (dialogError()) {
              <div class="notice error spaced" role="alert">{{ dialogError() }}</div>
            }
            <div class="form-actions">
              <button class="btn" type="button" (click)="salespersonDialog.set(null)">Cancel</button>
              <button class="btn btn-primary" type="submit" [disabled]="saving()">{{ saving() ? 'Saving...' : 'Save salesperson' }}</button>
            </div>
          </div>
        </form>
      </div>
    }

    @if (loginDialog(); as dialog) {
      <div class="backdrop" (click)="loginDialog.set(null)">
        <form class="dialog" role="dialog" aria-modal="true" aria-labelledby="login-title" (click)="$event.stopPropagation()" (ngSubmit)="saveLogin(dialog)">
          <div class="dialog-head">
            <div>
              <h2 id="login-title">Create login for {{ dialog.salesperson.name }}</h2>
            </div>
            <button class="icon-btn" type="button" (click)="loginDialog.set(null)" aria-label="Close">×</button>
          </div>
          <div class="dialog-body">
            <div class="form-grid">
              <label class="field full">Sign-in email<input name="email" type="email" required maxlength="100" [(ngModel)]="dialog.model.email"></label>
              <label class="field">Temporary password<input name="password" type="text" required minlength="8" maxlength="72" [(ngModel)]="dialog.model.password">
                <span class="hint">At least 8 characters. Share it with the salesperson.</span>
              </label>
              <label class="field">Role
                <select name="role" [(ngModel)]="dialog.model.role">
                  <option value="EMPLOYEE">Salesperson</option>
                  <option value="ADMIN">Administrator</option>
                </select>
              </label>
            </div>
            @if (dialogError()) {
              <div class="notice error spaced" role="alert">{{ dialogError() }}</div>
            }
            <div class="form-actions">
              <button class="btn" type="button" (click)="loginDialog.set(null)">Cancel</button>
              <button class="btn btn-primary" type="submit" [disabled]="saving()">Create login</button>
            </div>
          </div>
        </form>
      </div>
    }
  `,
  styles: `
    .spaced { margin-top: 14px; }
  `
})
export class StaffAdminComponent {
  private readonly api = inject(GroceryApiService);
  private readonly toast = inject(ToastService);
  readonly session = inject(SessionService);
  readonly catalog = inject(CatalogService);

  readonly storeId = signal<number | null>(this.catalog.storeId());
  readonly salespeople = signal<Salesperson[]>([]);
  readonly loading = signal(true);
  readonly salespersonDialog = signal<SalespersonDialog | null>(null);
  readonly loginDialog = signal<LoginDialog | null>(null);
  readonly dialogError = signal('');
  readonly saving = signal(false);
  readonly defaultStore = computed(() => this.storeId() ?? this.catalog.storeId());

  constructor() {
    effect(() => {
      const storeId = this.storeId();
      untracked(() => this.load(storeId));
    });
  }

  address(salesperson: Salesperson): string {
    return [salesperson.street, salesperson.city, salesperson.state, salesperson.zipCode].filter(Boolean).join(', ') || 'No address';
  }

  openSalesperson(salesperson: Salesperson | null): void {
    this.dialogError.set('');
    this.salespersonDialog.set({
      salespersonId: salesperson?.salespersonId ?? null,
      model: {
        name: salesperson?.name ?? '',
        street: salesperson?.street ?? '',
        city: salesperson?.city ?? '',
        state: salesperson?.state ?? 'PA',
        zipCode: salesperson?.zipCode ?? '',
        email: salesperson?.email ?? '',
        jobTitle: salesperson?.jobTitle ?? 'Sales Associate',
        salary: salesperson?.salary ?? null,
        storeId: salesperson?.storeId ?? this.defaultStore()
      }
    });
  }

  saveSalesperson(dialog: SalespersonDialog): void {
    const text = (value: string | null) => (value && value.trim() ? value.trim() : null);
    const salary = dialog.model.salary as unknown;
    const request: SalespersonRequest = {
      ...dialog.model,
      name: dialog.model.name.trim(),
      jobTitle: dialog.model.jobTitle.trim(),
      street: text(dialog.model.street),
      city: text(dialog.model.city),
      state: text(dialog.model.state),
      zipCode: text(dialog.model.zipCode),
      email: text(dialog.model.email),
      salary: salary === null || salary === '' ? null : Number(salary)
    };
    if (!request.name || !request.jobTitle) {
      this.dialogError.set('Name and job title are required.');
      return;
    }
    this.saving.set(true);
    this.dialogError.set('');
    const call =
      dialog.salespersonId === null
        ? this.api.createSalesperson(request)
        : this.api.updateSalesperson(dialog.salespersonId, request);
    call.subscribe({
      next: (salesperson) => {
        this.saving.set(false);
        this.salespersonDialog.set(null);
        this.toast.show(`${salesperson.name} was saved.`);
        this.catalog.reloadStores();
        this.load(this.storeId());
      },
      error: (error) => {
        this.saving.set(false);
        this.dialogError.set(errorMessage(error, 'The salesperson could not be saved.'));
      }
    });
  }

  openLogin(salesperson: Salesperson): void {
    this.dialogError.set('');
    this.loginDialog.set({
      salesperson,
      model: { email: salesperson.email ?? '', password: '', role: 'EMPLOYEE', salespersonId: salesperson.salespersonId }
    });
  }

  saveLogin(dialog: LoginDialog): void {
    if (dialog.model.password.length < 8) {
      this.dialogError.set('The password must be at least 8 characters.');
      return;
    }
    this.saving.set(true);
    this.api.createStaffAccount({ ...dialog.model, email: dialog.model.email.trim() }).subscribe({
      next: () => {
        this.saving.set(false);
        this.loginDialog.set(null);
        this.toast.show(`${dialog.salesperson.name} can now sign in as ${dialog.model.email.trim()}.`);
        this.load(this.storeId());
      },
      error: (error) => {
        this.saving.set(false);
        this.dialogError.set(errorMessage(error, 'The login could not be created.'));
      }
    });
  }

  private load(storeId: number | null): void {
    this.loading.set(true);
    this.api.getSalespeople(storeId).subscribe({
      next: (salespeople) => {
        this.salespeople.set(salespeople);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.toast.error(error, 'Staff could not be loaded.');
      }
    });
  }
}
