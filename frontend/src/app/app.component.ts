import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { ApiService } from './api.service';
import {
  Product, Inventory, Customer, CustomerDetails, CustomerRequest, HomeDetails,
  BusinessDetails, Order, OrderItem, ProductReport, CategoryReport,
  RegionReport, BusinessReport, CreateOrderRequest
} from './types';

type Tab = 'products' | 'cart' | 'customers' | 'inventory' | 'orders' | 'reports';
type CartLine = { productId: number; quantity: number };

function emptyHome(): HomeDetails { return { maritalStatus: null, gender: null, age: null, income: null }; }
function emptyBusiness(): BusinessDetails { return { businessCategory: null, annualIncome: null }; }
function emptyCustomer(): CustomerRequest {
  return { customerId: 0, name: '', street: '', city: '', state: '', zipCode: '', customerType: 'Home', home: emptyHome(), business: null };
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {
  private api = inject(ApiService);
  tabs: { id: Tab; label: string }[] = [
    { id: 'products', label: 'Browse products' }, { id: 'cart', label: 'Cart / checkout' },
    { id: 'customers', label: 'Customers' }, { id: 'inventory', label: 'Inventory' },
    { id: 'orders', label: 'Orders' }, { id: 'reports', label: 'Reports' }
  ];
  active: Tab = 'products';
  loading = false;
  error = '';
  success = '';
  products: Product[] = [];
  inventory: Inventory[] = [];
  customers: Customer[] = [];
  orders: Order[] = [];
  storeId = 1;
  searchText = '';
  filterCategory = 0;
  maxPrice: number | null = null;
  cart: CartLine[] = [];
  addQuantities: Record<number, number> = {};
  checkout = { orderId: 0, customerId: 0, salespersonId: null as number | null, paymentMethod: 'Credit Card' };
  customerForm: CustomerRequest = emptyCustomer();
  editingCustomer = false;
  selectedInventoryStore = 1;
  stockChanges: Record<number, number> = {};
  orderDetailId: number | null = null;
  orderItems: OrderItem[] = [];
  productReports: ProductReport[] = [];
  categoryReports: CategoryReport[] = [];
  regionReports: RegionReport[] = [];
  businessReports: BusinessReport[] = [];
  businessProductId = 0;
  reportsLoaded = false;

  ngOnInit(): void { this.reloadBase(); }

  money(value: number | null | undefined): string {
    if (value === null || value === undefined) return 'N/A';
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
  }

  private errorText(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) return 'Cannot reach the backend. Start Spring Boot on port 8080 and use npm start for Angular.';
      const data = error.error;
      return `${error.status}: ${typeof data === 'string' && data.length < 250 ? data : data?.detail || data?.message || data?.error || error.statusText}`;
    }
    return error instanceof Error ? error.message : 'Unexpected error';
  }

  private fail(error: unknown): void { this.loading = false; this.error = this.errorText(error); this.success = ''; }
  private okay(message: string): void { this.loading = false; this.error = ''; this.success = message; }
  clearMessages(): void { this.error = ''; this.success = ''; }

  selectTab(tab: Tab): void {
    this.active = tab;
    this.clearMessages();
    if (tab === 'reports' && !this.reportsLoaded) this.loadReports();
  }

  reloadBase(): void {
    this.loading = true;
    forkJoin({
      products: this.api.products(), inventory: this.api.inventory(),
      customers: this.api.customers(), orders: this.api.orders()
    }).subscribe({
      next: data => {
        this.products = data.products;
        this.inventory = data.inventory;
        this.customers = data.customers;
        this.orders = data.orders;
        if (!this.checkout.customerId && this.customers.length) this.checkout.customerId = this.customers[0].customerId;
        if (!this.checkout.orderId) this.checkout.orderId = this.nextOrderId();
        if (!this.businessProductId && this.products.length) this.businessProductId = this.products[0].productId;
        this.loading = false;
      }, error: error => this.fail(error)
    });
  }

  get categories(): number[] { return [...new Set(this.products.map(p => p.categoryId))].sort((a, b) => a - b); }
  get filteredProducts(): Product[] {
    const q = this.searchText.trim().toLowerCase();
    return this.products.filter(p =>
      (!q || p.productName.toLowerCase().includes(q) || (p.brand || '').toLowerCase().includes(q) || String(p.productId).includes(q)) &&
      (!this.filterCategory || p.categoryId === Number(this.filterCategory)) &&
      (this.maxPrice === null || p.price <= this.maxPrice)
    );
  }
  productName(id: number): string { return this.products.find(p => p.productId === id)?.productName || `Product ${id}`; }
  stock(store: number, product: number): number { return this.inventory.find(i => i.storeId === store && i.productId === product)?.quantity ?? 0; }
  get storeIds(): number[] { return [...new Set(this.inventory.map(i => i.storeId))].sort((a, b) => a - b); }
  get cartTotal(): number {
    return this.cart.reduce((sum, line) => sum + (this.products.find(p => p.productId === line.productId)?.price || 0) * line.quantity, 0);
  }
  get cartQuantity(): number { return this.cart.reduce((sum, line) => sum + line.quantity, 0); }
  cartPrice(productId: number): number { return this.products.find(p => p.productId === productId)?.price || 0; }
  changeStore(storeId: number): void {
    this.storeId = Number(storeId);
    // The cart is kept so users can see which items are unavailable at the new store.
    this.clearMessages();
  }
  addToCart(product: Product): void {
    const quantity = Number(this.addQuantities[product.productId] || 1);
    const existing = this.cart.find(x => x.productId === product.productId);
    const requested = (existing?.quantity || 0) + quantity;
    if (!Number.isInteger(quantity) || quantity <= 0) { this.error = 'Quantity must be a positive whole number.'; return; }
    if (requested > this.stock(this.storeId, product.productId)) { this.error = 'Not enough inventory at this store.'; return; }
    if (existing) existing.quantity = requested;
    else this.cart.push({ productId: product.productId, quantity });
    this.okay(`${product.productName} added to cart.`);
  }
  removeFromCart(productId: number): void { this.cart = this.cart.filter(x => x.productId !== productId); }
  nextOrderId(): number { return Math.max(1000, ...this.orders.map(o => o.orderId)) + 1; }
  placeOrder(): void {
    this.clearMessages();
    if (!this.cart.length) { this.error = 'Add at least one product before checkout.'; return; }
    if (!Number.isInteger(this.checkout.orderId) || this.checkout.orderId <= 0 || !this.checkout.customerId) {
      this.error = 'Select a customer and enter a valid order ID.'; return;
    }
    if (this.cart.some(x => x.quantity <= 0 || !Number.isInteger(x.quantity) || x.quantity > this.stock(this.storeId, x.productId))) {
      this.error = 'Cart quantities must be whole numbers and cannot exceed available stock.'; return;
    }
    const request: CreateOrderRequest = {
      orderId: this.checkout.orderId,
      customerId: Number(this.checkout.customerId), storeId: this.storeId,
      salespersonId: this.checkout.salespersonId, paymentMethod: this.checkout.paymentMethod,
      items: this.cart.map(x => ({ productId: x.productId, quantity: x.quantity }))
    };
    this.loading = true;
    this.api.createOrder(request).subscribe({
      next: order => {
        this.cart = [];
        this.okay(`Order #${order.orderId} placed. Payment method recorded (simulated payment only).`);
        this.checkout.orderId = 0;
        this.reloadBase();
      }, error: e => this.fail(e)
    });
  }

  onCustomerTypeChange(type: 'Home' | 'Business'): void {
    this.customerForm.customerType = type;
    this.customerForm.home = type === 'Home' ? emptyHome() : null;
    this.customerForm.business = type === 'Business' ? emptyBusiness() : null;
  }
  resetCustomer(): void { this.editingCustomer = false; this.customerForm = emptyCustomer(); this.clearMessages(); }
  editCustomer(customer: Customer): void {
    this.loading = true;
    this.clearMessages();
    this.api.customerDetails(customer.customerId).subscribe({
      next: details => {
        this.customerForm = {
          customerId: details.customerId, name: details.name,
          street: details.street, city: details.city, state: details.state,
          zipCode: details.zipCode, customerType: details.customerType,
          home: details.home, business: details.business
        };
        this.editingCustomer = true;
        this.loading = false;
      }, error: e => this.fail(e)
    });
  }
  saveCustomer(): void {
    const c = this.customerForm;
    this.clearMessages();
    if (!Number.isInteger(c.customerId) || c.customerId <= 0 || !c.name.trim()) {
      this.error = 'Customer ID must be a positive integer and name is required.'; return;
    }
    this.loading = true;
    const operation = this.editingCustomer ? this.api.updateCustomer(c.customerId, c) : this.api.createCustomer(c);
    operation.subscribe({
      next: () => {
        const message = this.editingCustomer ? `Customer ${c.customerId} updated.` : `Customer ${c.customerId} created.`;
        this.resetCustomer();
        this.okay(message);
        this.api.customers().subscribe({ next: data => this.customers = data, error: e => this.fail(e) });
      }, error: e => this.fail(e)
    });
  }
  deleteCustomer(customer: Customer): void {
    if (!window.confirm(`Delete customer ${customer.customerId} (${customer.name})? Customers with orders cannot be deleted.`)) return;
    this.loading = true;
    this.api.deleteCustomer(customer.customerId).subscribe({
      next: () => {
        this.customers = this.customers.filter(c => c.customerId !== customer.customerId);
        this.okay(`Customer ${customer.customerId} deleted.`);
      }, error: e => this.fail(e)
    });
  }

  get visibleInventory(): Inventory[] { return this.inventory.filter(i => i.storeId === Number(this.selectedInventoryStore)); }
  currentStockValue(row: Inventory): number { return this.stockChanges[row.productId] ?? row.quantity; }
  saveStock(row: Inventory): void {
    const quantity = Number(this.currentStockValue(row));
    if (!Number.isInteger(quantity) || quantity < 0) { this.error = 'Inventory must be a nonnegative whole number.'; return; }
    this.loading = true;
    this.api.changeStock(row.storeId, row.productId, quantity).subscribe({
      next: updated => {
        row.quantity = updated.quantity;
        delete this.stockChanges[row.productId];
        this.okay(`Stock saved for ${this.productName(row.productId)} at store ${row.storeId}.`);
      }, error: e => this.fail(e)
    });
  }

  viewOrderItems(order: Order): void {
    this.loading = true;
    this.orderDetailId = order.orderId;
    this.orderItems = [];
    this.api.orderItems(order.orderId).subscribe({
      next: data => { this.orderItems = data; this.loading = false; },
      error: e => this.fail(e)
    });
  }
  loadReports(): void {
    this.loading = true;
    this.clearMessages();
    forkJoin({ products: this.api.productReports(), categories: this.api.categoryReports(), regions: this.api.regionReports() })
      .subscribe({
        next: data => {
          this.productReports = data.products;
          this.categoryReports = data.categories;
          this.regionReports = data.regions;
          this.reportsLoaded = true;
          this.loading = false;
          this.loadBusinessReport();
        }, error: e => this.fail(e)
      });
  }
  loadBusinessReport(): void {
    if (!this.businessProductId) return;
    this.loading = true;
    this.api.businessReports(this.businessProductId).subscribe({
      next: data => { this.businessReports = data; this.loading = false; },
      error: e => this.fail(e)
    });
  }
}
