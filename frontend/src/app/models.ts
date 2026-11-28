export type Role = 'CUSTOMER' | 'EMPLOYEE' | 'ADMIN';
export type CustomerType = 'HOME' | 'BUSINESS';
export type OrderStatus = 'PENDING' | 'PAID' | 'PROCESSING' | 'COMPLETED' | 'CANCELLED';

export const ORDER_STATUSES: OrderStatus[] = ['PENDING', 'PAID', 'PROCESSING', 'COMPLETED', 'CANCELLED'];
export const PAYMENT_METHODS = ['Credit Card', 'Debit Card', 'Cash at Pickup', 'Business Invoice'] as const;
export type PaymentMethod = (typeof PAYMENT_METHODS)[number];

export interface AuthUser {
  accountId: number;
  email: string;
  role: Role;
  customerId: number | null;
  salespersonId: number | null;
  name: string;
  storeId: number | null;
}

export interface Category {
  categoryId: number;
  categoryName: string;
  description: string | null;
}

export interface Store {
  storeId: number;
  street: string;
  city: string;
  state: string;
  zipCode: string;
  manager: string | null;
  regionId: number;
  regionName: string;
  salespersonCount: number;
}

export interface StoreRequest {
  street: string;
  city: string;
  state: string;
  zipCode: string;
  manager: string | null;
  regionId: number;
}

export interface Region {
  regionId: number;
  regionName: string;
  regionManager: string | null;
  storeCount: number;
}

export interface RegionRequest {
  regionName: string;
  regionManager: string | null;
}

export interface Product {
  productId: number;
  productName: string;
  brand: string | null;
  price: number;
  categoryId: number;
  categoryName: string;
  quantity: number;
  imageUrl: string | null;
}

export type ProductSort = 'name' | 'price_asc' | 'price_desc' | 'popular';

export interface ProductQuery {
  storeId: number;
  search: string;
  categoryId: number | null;
  minPrice: number | null;
  maxPrice: number | null;
  inStock: boolean;
  sort: ProductSort;
}

export interface StoreAvailability {
  storeId: number;
  street: string;
  city: string;
  state: string;
  quantity: number;
}

export interface ProductWriteRequest {
  productName: string;
  brand: string | null;
  price: number;
  categoryId: number;
  storeId: number;
  quantity: number;
  unitCost: number | null;
  supplier: string | null;
}

export interface InventoryItem {
  productId: number;
  productName: string;
  brand: string | null;
  price: number;
  categoryId: number;
  categoryName: string;
  quantity: number | null;
  carried: boolean;
  lowStock: boolean;
  imageUrl: string | null;
  lastUnitCost: number | null;
  lastSupplier: string | null;
}

export interface Purchase {
  purchaseId: number;
  purchaseDate: string;
  storeId: number;
  storeCity: string;
  productId: number;
  productName: string;
  quantity: number;
  unitCost: number;
  totalCost: number;
  supplier: string | null;
}

export interface CategoryRequest {
  categoryName: string;
  description: string | null;
}

export interface CartItem {
  product: Product;
  quantity: number;
}

export interface CreateOrderRequest {
  customerId: number;
  storeId: number;
  paymentMethod: string;
  items: Array<{ productId: number; quantity: number }>;
}

export interface OrderItem {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface Order {
  orderId: number;
  orderDate: string;
  customerId: number;
  customerName: string;
  customerType: CustomerType;
  storeId: number;
  storeCity: string;
  salespersonId: number | null;
  salespersonName: string | null;
  paymentMethod: string;
  orderStatus: OrderStatus;
  items: OrderItem[];
  itemCount: number;
  total: number;
}

export interface ItemChangeResponse {
  order: Order;
  refund: number;
  unitsRemoved: number;
  orderCancelled: boolean;
}

export interface Salesperson {
  salespersonId: number;
  name: string;
  street: string | null;
  city: string | null;
  state: string | null;
  zipCode: string | null;
  email: string | null;
  jobTitle: string;
  salary: number | null;
  storeId: number;
  storeCity: string;
  accountEmail: string | null;
}

export interface SalespersonRequest {
  name: string;
  street: string | null;
  city: string | null;
  state: string | null;
  zipCode: string | null;
  email: string | null;
  jobTitle: string;
  salary: number | null;
  storeId: number;
}

export interface StaffAccountRequest {
  email: string;
  password: string;
  role: 'EMPLOYEE' | 'ADMIN';
  salespersonId: number | null;
}

export interface Customer {
  customerId: number;
  name: string;
  customerType: CustomerType;
  street: string | null;
  city: string | null;
  state: string | null;
  zipCode: string | null;
  maritalStatus: string | null;
  gender: string | null;
  age: number | null;
  income: number | null;
  businessCategory: string | null;
  annualIncome: number | null;
  orderCount: number;
  totalSpent: number;
  lastOrderDate: string | null;
  hasAccount: boolean;
}

export interface CustomerRequest {
  name: string;
  customerType: CustomerType;
  street: string | null;
  city: string | null;
  state: string | null;
  zipCode: string | null;
  maritalStatus: string | null;
  gender: string | null;
  age: number | null;
  income: number | null;
  businessCategory: string | null;
  annualIncome: number | null;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  customerType: CustomerType;
  businessCategory: string | null;
}

export interface ReportFilter {
  from: string | null;
  to: string | null;
  storeId: number | null;
  regionId: number | null;
}

export interface ReportOverview {
  revenue: number;
  collected: number;
  profit: number;
  orderCount: number;
  unitsSold: number;
  averageOrderValue: number;
  customerCount: number;
  cancelledOrders: number;
}

export interface ProductSales {
  productId: number;
  productName: string;
  brand: string | null;
  categoryName: string;
  unitsSold: number;
  orderCount: number;
  revenue: number;
  profit: number;
}

export interface CategorySales {
  categoryId: number;
  categoryName: string;
  productCount: number;
  unitsSold: number;
  orderCount: number;
  revenue: number;
  profit: number;
  revenueSharePercent: number;
}

export interface RegionSales {
  regionId: number;
  regionName: string;
  regionManager: string | null;
  storeCount: number;
  orderCount: number;
  unitsSold: number;
  revenue: number;
  profit: number;
  averageOrderValue: number;
  revenueSharePercent: number;
}

export interface StoreSales {
  storeId: number;
  street: string;
  city: string;
  regionName: string;
  manager: string | null;
  salespersonCount: number;
  orderCount: number;
  unitsSold: number;
  revenue: number;
  profit: number;
}

export interface BusinessBuyer {
  customerId: number;
  name: string;
  businessCategory: string;
  city: string | null;
  orderCount: number;
  unitsBought: number;
  totalSpent: number;
  lastOrderDate: string | null;
}

export interface BusinessBuyers {
  subject: string;
  buyers: BusinessBuyer[];
  byBusinessCategory: Array<{
    businessCategory: string;
    customerCount: number;
    unitsBought: number;
    totalSpent: number;
  }>;
}

export interface SalespersonSales {
  salespersonId: number;
  name: string;
  jobTitle: string;
  storeId: number;
  storeCity: string;
  orderCount: number;
  unitsSold: number;
  revenue: number;
  averageOrderValue: number;
}

export interface MonthlySales {
  month: string;
  orderCount: number;
  unitsSold: number;
  revenue: number;
  profit: number;
}

export interface SegmentSales {
  segment: string;
  customerCount: number;
  orderCount: number;
  revenue: number;
  averageOrderValue: number;
  revenueSharePercent: number;
}

export interface CustomerSegments {
  customerTypes: SegmentSales[];
  ageBands: SegmentSales[];
  incomeBands: SegmentSales[];
}

export interface TopCustomer {
  customerId: number;
  name: string;
  customerType: CustomerType;
  orderCount: number;
  unitsBought: number;
  totalSpent: number;
  lastOrderDate: string | null;
}

export interface PaymentMethodSales {
  paymentMethod: string;
  orderCount: number;
  revenue: number;
  revenueSharePercent: number;
}
