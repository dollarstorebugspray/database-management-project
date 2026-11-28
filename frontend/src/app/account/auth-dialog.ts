import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SessionService } from '../core/session.service';
import { errorMessage } from '../core/toast.service';
import { CustomerType } from '../models';

const DEMO_ACCOUNTS = [
  { label: 'Home customer', email: 'customer@grocerystore.local', password: 'Customer123!' },
  { label: 'Business customer', email: 'business@grocerystore.local', password: 'Business123!' },
  { label: 'Salesperson', email: 'salesperson@grocerystore.local', password: 'Salesperson123!' },
  { label: 'Administrator', email: 'admin@grocerystore.local', password: 'Admin123!' }
];

@Component({
  selector: 'app-auth-dialog',
  imports: [FormsModule],
  template: `
    <div class="backdrop" (click)="close()">
      <div class="dialog" role="dialog" aria-modal="true" aria-labelledby="auth-title" (click)="$event.stopPropagation()">
        <div class="dialog-head">
          <div>
            <h2 id="auth-title">{{ mode() === 'login' ? 'Sign in' : 'Create an account' }}</h2>
          </div>
          <button class="icon-btn" type="button" (click)="close()" aria-label="Close">×</button>
        </div>
        <div class="dialog-body">
          <div class="segmented tabs">
            <button type="button" [class.active]="mode() === 'login'" (click)="switchTo('login')">Sign in</button>
            <button type="button" [class.active]="mode() === 'register'" (click)="switchTo('register')">Create account</button>
          </div>

          @if (error()) {
            <div class="notice error" role="alert">{{ error() }}</div>
          }

          @if (mode() === 'login') {
            <form class="auth-form" (ngSubmit)="login()">
              <label class="field">
                Email
                <input name="email" type="email" autocomplete="username" required [(ngModel)]="email">
              </label>
              <label class="field">
                Password
                <input name="password" type="password" autocomplete="current-password" required [(ngModel)]="password">
              </label>
              <button class="btn btn-primary" type="submit" [disabled]="busy()">{{ busy() ? 'Signing in...' : 'Sign in' }}</button>
            </form>

            <div class="demo">
              <p class="small muted">Demo accounts (click to fill in):</p>
              <div class="demo-grid">
                @for (account of demoAccounts; track account.email) {
                  <button type="button" class="btn btn-sm" (click)="fill(account.email, account.password)">{{ account.label }}</button>
                }
              </div>
            </div>
          } @else {
            <form class="auth-form" (ngSubmit)="register()">
              <label class="field">
                Name {{ customerType === 'BUSINESS' ? '(business name)' : '' }}
                <input name="name" required maxlength="50" autocomplete="name" [(ngModel)]="name">
              </label>
              <label class="field">
                Email
                <input name="email" type="email" required maxlength="100" autocomplete="email" [(ngModel)]="email">
              </label>
              <label class="field">
                Password
                <input name="password" type="password" required minlength="8" maxlength="72" autocomplete="new-password" [(ngModel)]="password">
                <span class="hint">At least 8 characters.</span>
              </label>
              <div class="field">
                Account type
                <div class="segmented">
                  <button type="button" [class.active]="customerType === 'HOME'" (click)="customerType = 'HOME'">Home</button>
                  <button type="button" [class.active]="customerType === 'BUSINESS'" (click)="customerType = 'BUSINESS'">Business</button>
                </div>
              </div>
              @if (customerType === 'BUSINESS') {
                <label class="field">
                  Business category
                  <input name="businessCategory" required maxlength="50" placeholder="e.g. Restaurant, Cafe, Office" [(ngModel)]="businessCategory">
                </label>
              }
              <button class="btn btn-primary" type="submit" [disabled]="busy()">{{ busy() ? 'Creating account...' : 'Create account' }}</button>
            </form>
          }
        </div>
      </div>
    </div>
  `,
  styles: `
    .tabs { margin-bottom: 16px; }
    .auth-form { display: grid; gap: 12px; margin-top: 12px; }
    .demo { margin-top: 18px; padding-top: 14px; border-top: 1px solid var(--line); }
    .demo-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; margin-top: 8px; }
  `
})
export class AuthDialogComponent {
  private readonly session = inject(SessionService);

  readonly demoAccounts = DEMO_ACCOUNTS;
  readonly mode = signal<'login' | 'register'>(this.session.authDialog() ?? 'login');
  readonly busy = signal(false);
  readonly error = signal('');

  email = '';
  password = '';
  name = '';
  customerType: CustomerType = 'HOME';
  businessCategory = '';

  switchTo(mode: 'login' | 'register'): void {
    this.mode.set(mode);
    this.error.set('');
  }

  fill(email: string, password: string): void {
    this.email = email;
    this.password = password;
    this.error.set('');
  }

  close(): void {
    this.session.authDialog.set(null);
  }

  login(): void {
    if (!this.email.trim() || !this.password) {
      this.error.set('Enter your email and password.');
      return;
    }
    this.busy.set(true);
    this.session.login(this.email.trim(), this.password).subscribe({
      next: () => this.busy.set(false),
      error: (error) => {
        this.busy.set(false);
        this.error.set(errorMessage(error, 'Sign-in failed.'));
      }
    });
  }

  register(): void {
    if (!this.name.trim() || !this.email.trim()) {
      this.error.set('Enter your name and email.');
      return;
    }
    if (this.password.length < 8) {
      this.error.set('The password must be at least 8 characters.');
      return;
    }
    if (this.customerType === 'BUSINESS' && !this.businessCategory.trim()) {
      this.error.set('Enter the business category.');
      return;
    }
    this.busy.set(true);
    this.session
      .register({
        name: this.name.trim(),
        email: this.email.trim(),
        password: this.password,
        customerType: this.customerType,
        businessCategory: this.customerType === 'BUSINESS' ? this.businessCategory.trim() : null
      })
      .subscribe({
        next: () => this.busy.set(false),
        error: (error) => {
          this.busy.set(false);
          this.error.set(errorMessage(error, 'The account could not be created.'));
        }
      });
  }
}
