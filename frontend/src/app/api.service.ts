import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BusinessReport, CategoryReport, CreateOrderRequest, Customer, CustomerDetails, CustomerRequest, Inventory, Order, OrderItem, Product, ProductReport, RegionReport } from './types';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  products(): Observable<Product[]> { return this.http.get<Product[]>('/api/products'); }
  inventory(): Observable<Inventory[]> { return this.http.get<Inventory[]>('/api/inventory'); }
  changeStock(storeId: number, productId: number, quantity: number): Observable<Inventory> {
    return this.http.patch<Inventory>(`/api/inventory/store/${storeId}/product/${productId}`, { quantity });
  }
  customers(): Observable<Customer[]> { return this.http.get<Customer[]>('/api/customers'); }
  customerDetails(id: number): Observable<CustomerDetails> { return this.http.get<CustomerDetails>(`/api/customers/${id}/details`); }
  createCustomer(body: CustomerRequest): Observable<Customer> { return this.http.post<Customer>('/api/customers', body); }
  updateCustomer(id: number, body: CustomerRequest): Observable<Customer> { return this.http.put<Customer>(`/api/customers/${id}`, body); }
  deleteCustomer(id: number): Observable<void> { return this.http.delete<void>(`/api/customers/${id}`); }
  orders(): Observable<Order[]> { return this.http.get<Order[]>('/api/orders'); }
  orderItems(orderId: number): Observable<OrderItem[]> { return this.http.get<OrderItem[]>(`/api/order-items/order/${orderId}`); }
  createOrder(body: CreateOrderRequest): Observable<Order> { return this.http.post<Order>('/api/orders', body); }
  productReports(): Observable<ProductReport[]> { return this.http.get<ProductReport[]>('/api/reports/products'); }
  categoryReports(): Observable<CategoryReport[]> { return this.http.get<CategoryReport[]>('/api/reports/categories'); }
  regionReports(): Observable<RegionReport[]> { return this.http.get<RegionReport[]>('/api/reports/regions'); }
  businessReports(productId: number): Observable<BusinessReport[]> {
    return this.http.get<BusinessReport[]>(`/api/reports/businesses?productId=${productId}`);
  }
}
