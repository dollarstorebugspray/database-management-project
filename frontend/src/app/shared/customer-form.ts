import { Component, input } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CustomerRequest } from '../models';

@Component({
  selector: 'app-customer-form',
  imports: [FormsModule],
  template: `
    <div class="form-grid">
      <label class="field full">
        Name
        <input name="cfName" required maxlength="50" [(ngModel)]="model().name">
      </label>
      @if (allowTypeChange()) {
        <div class="field">
          Customer type
          <div class="segmented">
            <button type="button" [class.active]="model().customerType === 'HOME'" (click)="model().customerType = 'HOME'">Home</button>
            <button type="button" [class.active]="model().customerType === 'BUSINESS'" (click)="model().customerType = 'BUSINESS'">Business</button>
          </div>
        </div>
      }
      <label class="field full">
        Street
        <input name="cfStreet" maxlength="50" [(ngModel)]="model().street">
      </label>
      <label class="field">
        City
        <input name="cfCity" maxlength="25" [(ngModel)]="model().city">
      </label>
      <label class="field">
        State
        <input name="cfState" maxlength="15" [(ngModel)]="model().state">
      </label>
      <label class="field">
        ZIP code
        <input name="cfZip" maxlength="10" [(ngModel)]="model().zipCode">
      </label>

      @if (model().customerType === 'HOME') {
        <label class="field">
          Marital status
          <select name="cfMarital" [(ngModel)]="model().maritalStatus">
            <option [ngValue]="null">Prefer not to say</option>
            <option>Single</option>
            <option>Married</option>
            <option>Divorced</option>
            <option>Widowed</option>
          </select>
        </label>
        <label class="field">
          Gender
          <select name="cfGender" [(ngModel)]="model().gender">
            <option [ngValue]="null">-</option>
            <option>Female</option>
            <option>Male</option>
            <option>Non-binary</option>
            <option>Prefer not to say</option>
          </select>
        </label>
        <label class="field">
          Age
          <input name="cfAge" type="number" min="0" max="130" [(ngModel)]="model().age">
        </label>
        <label class="field">
          Annual income ($)
          <input name="cfIncome" type="number" min="0" step="1000" [(ngModel)]="model().income">
        </label>
      } @else {
        <label class="field">
          Business category
          <input name="cfBusinessCategory" required maxlength="50" placeholder="Restaurant, Cafe, Office..." [(ngModel)]="model().businessCategory">
        </label>
        <label class="field">
          Gross annual income ($)
          <input name="cfAnnualIncome" type="number" min="0" step="1000" [(ngModel)]="model().annualIncome">
        </label>
      }
    </div>
  `
})
export class CustomerFormComponent {
  readonly model = input.required<CustomerRequest>();
  readonly allowTypeChange = input(true);
}

export function emptyCustomer(): CustomerRequest {
  return {
    name: '',
    customerType: 'HOME',
    street: '',
    city: '',
    state: 'PA',
    zipCode: '',
    maritalStatus: null,
    gender: null,
    age: null,
    income: null,
    businessCategory: null,
    annualIncome: null
  };
}

export function cleanCustomer(model: CustomerRequest): CustomerRequest {
  const number = (value: unknown) =>
    value === null || value === undefined || value === '' ? null : Number(value);
  const text = (value: string | null) => (value && value.trim() ? value.trim() : null);
  const home = model.customerType === 'HOME';
  return {
    name: model.name.trim(),
    customerType: model.customerType,
    street: text(model.street),
    city: text(model.city),
    state: text(model.state),
    zipCode: text(model.zipCode),
    maritalStatus: home ? text(model.maritalStatus) : null,
    gender: home ? text(model.gender) : null,
    age: home ? number(model.age) : null,
    income: home ? number(model.income) : null,
    businessCategory: home ? null : text(model.businessCategory),
    annualIncome: home ? null : number(model.annualIncome)
  };
}
