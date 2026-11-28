import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AuthUser,
  BusinessBuyers,
  Category,
  CategoryRequest,
  CategorySales,
  CreateOrderRequest,
  Customer,
  CustomerRequest,
  CustomerSegments,
  ItemChangeResponse,
  Salesperson,
  SalespersonRequest,
  InventoryItem,
  MonthlySales,
  Order,
  OrderStatus,
  PaymentMethodSales,
  Product,
  ProductQuery,
  ProductSales,
  ProductWriteRequest,
  Purchase,
  Region,
  RegionRequest,
  RegionSales,
  RegisterRequest,
  ReportFilter,
  ReportOverview,
  SalespersonSales,
  StaffAccountRequest,
  Store,
  StoreAvailability,
  StoreRequest,
  StoreSales,
  TopCustomer
} from './models';

function params(values: Record<string, string | number | boolean | null | undefined>): HttpParams {
  let result = new HttpParams();
  for (const [key, value] of Object.entries(values)) {
    if (value !== null && value !== undefined && value !== '') {
      result = result.set(key, String(value));
    }
  }
  return result;
}

@Injectable({ providedIn: 'root' })
export class GroceryApiService {
  private readonly http = inject(HttpClient);

  me(): Observable<AuthUser> {
    return this.http.get<AuthUser>('/api/auth/me');
  }

  login(email: string, password: string): Observable<AuthUser> {
    return this.http.post<AuthUser>('/api/auth/login', { email, password });
  }

  register(request: RegisterRequest): Observable<AuthUser> {
    return this.http.post<AuthUser>('/api/auth/register', request);
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', {});
  }

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>('/api/categories');
  }

  getStores(): Observable<Store[]> {
    return this.http.get<Store[]>('/api/stores');
  }

  getRegions(): Observable<Region[]> {
    return this.http.get<Region[]>('/api/regions');
  }

  getProducts(query: ProductQuery): Observable<Product[]> {
    return this.http.get<Product[]>('/api/products', {
      params: params({
        storeId: query.storeId,
        search: query.search.trim(),
        categoryId: query.categoryId,
        minPrice: query.minPrice,
        maxPrice: query.maxPrice,
        inStock: query.inStock || null,
        sort: query.sort
      })
    });
  }

  getAvailability(productId: number): Observable<StoreAvailability[]> {
    return this.http.get<StoreAvailability[]>(`/api/products/${productId}/availability`);
  }

  createOrder(request: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>('/api/orders', request);
  }

  cancelOrder(orderId: number): Observable<Order> {
    return this.http.post<Order>(`/api/orders/${orderId}/cancel`, {});
  }

  getStaffOrders(filter: { storeId: number | null; status: string; search: string }): Observable<Order[]> {
    return this.http.get<Order[]>('/api/admin/orders', {
      params: params({ storeId: filter.storeId, status: filter.status, search: filter.search.trim(), limit: 200 })
    });
  }

  updateOrderStatus(orderId: number, status: OrderStatus): Observable<Order> {
    return this.http.put<Order>(`/api/admin/orders/${orderId}/status`, { status });
  }

  cancelItems(orderId: number, items: Array<{ productId: number; quantity: number }>): Observable<ItemChangeResponse> {
    return this.http.post<ItemChangeResponse>(`/api/orders/${orderId}/cancel-items`, { items });
  }

  returnItems(
    orderId: number,
    items: Array<{ productId: number; quantity: number }>,
    restock: boolean
  ): Observable<ItemChangeResponse> {
    return this.http.post<ItemChangeResponse>(`/api/admin/orders/${orderId}/return-items`, { items, restock });
  }

  assignSalesperson(orderId: number, salespersonId: number): Observable<Order> {
    return this.http.put<Order>(`/api/admin/orders/${orderId}/salesperson`, { salespersonId });
  }

  getCustomers(search = '', type = ''): Observable<Customer[]> {
    return this.http.get<Customer[]>('/api/customers', { params: params({ search: search.trim(), type }) });
  }

  getCustomer(customerId: number): Observable<Customer> {
    return this.http.get<Customer>(`/api/customers/${customerId}`);
  }

  createCustomer(request: CustomerRequest): Observable<Customer> {
    return this.http.post<Customer>('/api/customers', request);
  }

  updateCustomer(customerId: number, request: CustomerRequest): Observable<Customer> {
    return this.http.put<Customer>(`/api/customers/${customerId}`, request);
  }

  deleteCustomer(customerId: number): Observable<void> {
    return this.http.delete<void>(`/api/customers/${customerId}`);
  }

  getCustomerOrders(customerId: number): Observable<Order[]> {
    return this.http.get<Order[]>(`/api/customers/${customerId}/orders`);
  }

  getInventory(storeId: number, search: string, lowStockOnly: boolean): Observable<InventoryItem[]> {
    return this.http.get<InventoryItem[]>('/api/admin/inventory', {
      params: params({ storeId, search: search.trim(), lowStockOnly: lowStockOnly || null })
    });
  }

  restock(storeId: number, productId: number, quantity: number, unitCost: number, supplier: string): Observable<Product> {
    return this.http.post<Product>('/api/admin/inventory/restock', {
      storeId,
      productId,
      quantity,
      unitCost,
      supplier: supplier.trim() || null
    });
  }

  getPurchases(storeId: number, search: string): Observable<Purchase[]> {
    return this.http.get<Purchase[]>('/api/admin/purchases', { params: params({ storeId, search: search.trim() }) });
  }

  createProduct(request: ProductWriteRequest): Observable<Product> {
    return this.http.post<Product>('/api/admin/products', request);
  }

  updateProduct(productId: number, request: ProductWriteRequest): Observable<Product> {
    return this.http.put<Product>(`/api/admin/products/${productId}`, request);
  }

  createCategory(request: CategoryRequest): Observable<Category> {
    return this.http.post<Category>('/api/admin/categories', request);
  }

  getSalespeople(storeId: number | null): Observable<Salesperson[]> {
    return this.http.get<Salesperson[]>('/api/admin/salespeople', { params: params({ storeId }) });
  }

  createSalesperson(request: SalespersonRequest): Observable<Salesperson> {
    return this.http.post<Salesperson>('/api/admin/salespeople', request);
  }

  updateSalesperson(salespersonId: number, request: SalespersonRequest): Observable<Salesperson> {
    return this.http.put<Salesperson>(`/api/admin/salespeople/${salespersonId}`, request);
  }

  createStaffAccount(request: StaffAccountRequest): Observable<unknown> {
    return this.http.post('/api/admin/accounts', request);
  }

  createStore(request: StoreRequest): Observable<Store> {
    return this.http.post<Store>('/api/admin/stores', request);
  }

  updateStore(storeId: number, request: StoreRequest): Observable<Store> {
    return this.http.put<Store>(`/api/admin/stores/${storeId}`, request);
  }

  createRegion(request: RegionRequest): Observable<Region> {
    return this.http.post<Region>('/api/admin/regions', request);
  }

  updateRegion(regionId: number, request: RegionRequest): Observable<Region> {
    return this.http.put<Region>(`/api/admin/regions/${regionId}`, request);
  }

  private report<T>(path: string, filter: ReportFilter, extra: Record<string, string | number | null> = {}): Observable<T> {
    return this.http.get<T>(`/api/admin/reports/${path}`, { params: params({ ...filter, ...extra }) });
  }

  reportOverview(filter: ReportFilter): Observable<ReportOverview> {
    return this.report('overview', filter);
  }

  reportProducts(filter: ReportFilter): Observable<ProductSales[]> {
    return this.report('products', filter);
  }

  reportCategories(filter: ReportFilter): Observable<CategorySales[]> {
    return this.report('categories', filter);
  }

  reportRegions(filter: ReportFilter): Observable<RegionSales[]> {
    return this.report('regions', filter);
  }

  reportStores(filter: ReportFilter): Observable<StoreSales[]> {
    return this.report('stores', filter);
  }

  reportBusinessBuyers(filter: ReportFilter, productId: number | null, categoryId: number | null): Observable<BusinessBuyers> {
    return this.report('business-buyers', filter, { productId, categoryId, limit: 10 });
  }

  reportSalespeople(filter: ReportFilter): Observable<SalespersonSales[]> {
    return this.report('salespeople', filter);
  }

  reportMonthly(filter: ReportFilter): Observable<MonthlySales[]> {
    return this.report('monthly', filter);
  }

  reportSegments(filter: ReportFilter): Observable<CustomerSegments> {
    return this.report('customer-segments', filter);
  }

  reportTopCustomers(filter: ReportFilter): Observable<TopCustomer[]> {
    return this.report('top-customers', filter, { limit: 10 });
  }

  reportPaymentMethods(filter: ReportFilter): Observable<PaymentMethodSales[]> {
    return this.report('payment-methods', filter);
  }

}
