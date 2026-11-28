import { Component, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SessionService } from '../core/session.service';
import { errorMessage, ToastService } from '../core/toast.service';
import { GroceryApiService } from '../grocery-api.service';
import { CustomerRequest } from '../models';
import { cleanCustomer, CustomerFormComponent } from '../shared/customer-form';

@Component({
  selector: 'app-profile',
  imports: [FormsModule, CustomerFormComponent],
  template: `
    <div class="page narrow">
      <div class="page-head">
        <div>
          <h1>Profile</h1>
        </div>
      </div>

      @if (form(); as model) {
        <form class="card card-pad" (ngSubmit)="save()">
          <p class="small muted type-note">
            {{ model.customerType === 'HOME' ? 'Home' : 'Business' }} customer ·
            {{ session.user()?.email }}
            · To switch between home and business, ask a staff member.
          </p>
          <app-customer-form [model]="model" [allowTypeChange]="false" />
          @if (error()) {
            <div class="notice error spaced" role="alert">{{ error() }}</div>
          }
          <div class="form-actions">
            <button class="btn btn-primary" type="submit" [disabled]="saving()">{{ saving() ? 'Saving...' : 'Save changes' }}</button>
          </div>
        </form>
      } @else {
        <p class="muted">Loading your profile...</p>
      }
    </div>
  `,
  styles: `
    .narrow { width: min(760px, 100%); }
    .type-note { margin-bottom: 14px; }
    .spaced { margin-top: 14px; }
  `
})
export class ProfileComponent {
  private readonly api = inject(GroceryApiService);
  private readonly toast = inject(ToastService);
  readonly session = inject(SessionService);

  readonly form = signal<CustomerRequest | null>(null);
  readonly saving = signal(false);
  readonly error = signal('');

  constructor() {
    effect(() => {
      const customer = this.session.customer();
      if (customer) {
        untracked(() =>
          this.form.set({
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
          })
        );
      }
    });
  }

  save(): void {
    const model = this.form();
    const customerId = this.session.user()?.customerId;
    if (!model || !customerId) {
      return;
    }
    if (!model.name.trim()) {
      this.error.set('Name is required.');
      return;
    }
    this.saving.set(true);
    this.error.set('');
    this.api.updateCustomer(customerId, cleanCustomer(model)).subscribe({
      next: () => {
        this.saving.set(false);
        this.session.refreshCustomer();
        this.toast.show('Your profile was saved.');
      },
      error: (error) => {
        this.saving.set(false);
        this.error.set(errorMessage(error, 'Your profile could not be saved.'));
      }
    });
  }
}
