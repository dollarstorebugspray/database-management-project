export interface Product { productId: number; productName: string; brand: string | null; price: number; costPrice: number | null; categoryId: number; }
export interface Inventory { storeId: number; productId: number; quantity: number; }
export interface Customer { customerId: number; name: string; street: string | null; city: string | null; state: string | null; zipCode: string | null; customerType: 'Home' | 'Business'; }
export interface HomeDetails { maritalStatus: string | null; gender: string | null; age: number | null; income: number | null; }
export interface BusinessDetails { businessCategory: string | null; annualIncome: number | null; }
export interface CustomerDetails extends Customer { home: HomeDetails | null; business: BusinessDetails | null; }
export interface CustomerRequest extends Customer { home: HomeDetails | null; business: BusinessDetails | null; }
export interface OrderItemRequest { productId: number; quantity: number; }
export interface CreateOrderRequest { orderId: number; customerId: number; storeId: number; salespersonId: number | null; paymentMethod: string; items: OrderItemRequest[]; }
export interface Order { orderId: number; orderDate: string; paymentMethod: string; customerId: number; salespersonId: number | null; storeId: number; }
export interface OrderItem { orderId: number; productId: number; quantity: number; salePrice: number; unitCost: number | null; }
export interface ProductReport { productId: number; productName: string; totalQuantitySold: number; totalRevenue: number; totalGrossProfit: number | null; salesMissingCost: number; }
export interface CategoryReport { categoryId: number; categoryName: string; totalQuantitySold: number; totalRevenue: number; }
export interface RegionReport { regionId: number; regionName: string; totalOrders: number; totalQuantitySold: number; totalRevenue: number; }
export interface BusinessReport { customerId: number; businessName: string; productId: number; productName: string; totalQuantityPurchased: number; totalAmountSpent: number; }
