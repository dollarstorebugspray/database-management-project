import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { CatalogService } from '../core/catalog.service';
import { errorMessage } from '../core/toast.service';
import { GroceryApiService } from '../grocery-api.service';
import {
  BusinessBuyers,
  CategorySales,
  CustomerSegments,
  MonthlySales,
  PaymentMethodSales,
  ProductSales,
  Region,
  RegionSales,
  ReportFilter,
  ReportOverview,
  SalespersonSales,
  SegmentSales,
  StoreSales,
  TopCustomer
} from '../models';
import { BarListComponent, ChartDatum, ColumnChartComponent, SegmentBarComponent } from '../shared/charts';
import { axisMoney, count, isoDate, money, moneyCompact, monthLabel, percent } from '../shared/format';

type Preset = 'all' | '30' | '90' | 'ytd' | 'custom';
type ProductSortKey = 'revenue' | 'unitsSold' | 'profit' | 'productName';

interface DashboardData {
  overview: ReportOverview;
  monthly: MonthlySales[];
  categories: CategorySales[];
  regions: RegionSales[];
  stores: StoreSales[];
  products: ProductSales[];
  salespeople: SalespersonSales[];
  segments: CustomerSegments;
  payments: PaymentMethodSales[];
  topCustomers: TopCustomer[];
}

@Component({
  selector: 'app-dashboard',
  imports: [FormsModule, CurrencyPipe, DecimalPipe, DatePipe, BarListComponent, ColumnChartComponent, SegmentBarComponent],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardComponent {
  private readonly api = inject(GroceryApiService);
  readonly catalog = inject(CatalogService);

  readonly presets: Array<{ id: Preset; label: string }> = [
    { id: 'all', label: 'All time' },
    { id: '30', label: 'Last 30 days' },
    { id: '90', label: 'Last 90 days' },
    { id: 'ytd', label: 'Year to date' },
    { id: 'custom', label: 'Custom' }
  ];
  readonly preset = signal<Preset>('all');
  readonly customFrom = signal('');
  readonly customTo = signal('');
  readonly regionId = signal<number | null>(null);
  readonly storeId = signal<number | null>(null);

  readonly regions = signal<Region[]>([]);
  readonly data = signal<DashboardData | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');

  readonly buyerSubject = signal('all');
  readonly buyers = signal<BusinessBuyers | null>(null);
  readonly buyersLoading = signal(false);

  readonly productSort = signal<ProductSortKey>('revenue');
  readonly showAllProducts = signal(false);

  readonly storesInRegion = computed(() =>
    this.catalog.stores().filter((store) => this.regionId() === null || store.regionId === this.regionId())
  );

  readonly filter = computed<ReportFilter>(() => {
    let from: string | null = null;
    let to: string | null = null;
    switch (this.preset()) {
      case '30':
        from = isoDate(-29);
        break;
      case '90':
        from = isoDate(-89);
        break;
      case 'ytd':
        from = `${new Date().getFullYear()}-01-01`;
        break;
      case 'custom':
        from = this.customFrom() || null;
        to = this.customTo() || null;
        break;
    }
    return { from, to, storeId: this.storeId(), regionId: this.regionId() };
  });

  readonly rangeError = computed(() => {
    const { from, to } = this.filter();
    return from && to && from > to ? 'The start date is after the end date.' : '';
  });

  readonly scopeLabel = computed(() => {
    const store = this.catalog.stores().find((item) => item.storeId === this.storeId());
    const region = this.regions().find((item) => item.regionId === this.regionId());
    const place = store ? `Store #${store.storeId} ${store.city}` : region ? `${region.regionName} region` : 'All stores';
    const { from, to } = this.filter();
    const period = from || to ? `${from ?? 'start'} to ${to ?? 'today'}` : 'all time';
    return `${place} · ${period}`;
  });

  readonly monthlyChart = computed(() =>
    (this.data()?.monthly ?? []).map((row) => ({
      label: monthLabel(row.month),
      short: monthLabel(row.month, true),
      value: Number(row.revenue),
      display: moneyCompact(row.revenue),
      details: [
        { label: 'Orders', value: count(row.orderCount) },
        { label: 'Units', value: count(row.unitsSold) },
        { label: 'Profit', value: moneyCompact(row.profit) }
      ]
    }))
  );

  readonly categoryChart = computed<ChartDatum[]>(() =>
    (this.data()?.categories ?? []).map((row) => ({
      label: row.categoryName,
      value: Number(row.revenue),
      display: moneyCompact(row.revenue),
      details: [
        { label: 'Share', value: percent(row.revenueSharePercent) },
        { label: 'Units', value: count(row.unitsSold) },
        { label: 'Profit', value: moneyCompact(row.profit) }
      ]
    }))
  );

  readonly regionChart = computed<ChartDatum[]>(() =>
    (this.data()?.regions ?? []).map((row) => ({
      label: row.regionName,
      sublabel: `${row.storeCount} store${row.storeCount === 1 ? '' : 's'} · ${row.regionManager ?? 'no manager'}`,
      value: Number(row.revenue),
      display: moneyCompact(row.revenue),
      details: [
        { label: 'Share', value: percent(row.revenueSharePercent) },
        { label: 'Orders', value: count(row.orderCount) },
        { label: 'Units', value: count(row.unitsSold) },
        { label: 'Avg order', value: money(row.averageOrderValue) },
        { label: 'Profit', value: moneyCompact(row.profit) }
      ]
    }))
  );

  readonly storeChart = computed<ChartDatum[]>(() =>
    (this.data()?.stores ?? []).map((row) => ({
      label: `#${row.storeId} ${row.city}`,
      sublabel: `${row.street} · ${row.regionName}`,
      value: Number(row.revenue),
      display: moneyCompact(row.revenue),
      details: [
        { label: 'Orders', value: count(row.orderCount) },
        { label: 'Salespeople', value: count(row.salespersonCount) },
        { label: 'Profit', value: moneyCompact(row.profit) }
      ]
    }))
  );

  readonly salespeopleChart = computed<ChartDatum[]>(() =>
    (this.data()?.salespeople ?? []).slice(0, 10).map((row) => ({
      label: row.name,
      sublabel: `${row.jobTitle} · #${row.storeId} ${row.storeCity}`,
      value: Number(row.revenue),
      display: moneyCompact(row.revenue),
      details: [
        { label: 'Orders', value: count(row.orderCount) },
        { label: 'Units', value: count(row.unitsSold) },
        { label: 'Avg order', value: money(row.averageOrderValue) }
      ]
    }))
  );

  readonly customerTypeChart = computed<ChartDatum[]>(() =>
    this.segmentData(this.data()?.segments.customerTypes ?? [], (name) =>
      name === 'HOME' ? 'Home customers' : 'Business customers'
    )
  );
  readonly ageChart = computed(() => this.segmentData(this.data()?.segments.ageBands ?? []));
  readonly incomeChart = computed(() => this.segmentData(this.data()?.segments.incomeBands ?? []));

  readonly paymentChart = computed<ChartDatum[]>(() =>
    (this.data()?.payments ?? []).map((row) => ({
      label: row.paymentMethod,
      value: Number(row.revenue),
      display: moneyCompact(row.revenue),
      details: [
        { label: 'Share', value: percent(row.revenueSharePercent) },
        { label: 'Orders', value: count(row.orderCount) }
      ]
    }))
  );

  readonly buyerChart = computed<ChartDatum[]>(() =>
    (this.buyers()?.buyers ?? []).map((row) => ({
      label: row.name,
      sublabel: `${row.businessCategory}${row.city ? ' · ' + row.city : ''}`,
      value: Number(row.unitsBought),
      display: `${count(row.unitsBought)} units`,
      details: [
        { label: 'Spent', value: money(row.totalSpent) },
        { label: 'Orders', value: count(row.orderCount) },
        { label: 'Last order', value: row.lastOrderDate ?? '-' }
      ]
    }))
  );

  readonly productOptions = computed(() =>
    [...(this.data()?.products ?? [])].sort((a, b) => a.productName.localeCompare(b.productName))
  );

  readonly productRows = computed(() => {
    const key = this.productSort();
    const rows = [...(this.data()?.products ?? [])];
    rows.sort((a, b) =>
      key === 'productName' ? a.productName.localeCompare(b.productName) : Number(b[key]) - Number(a[key])
    );
    return this.showAllProducts() ? rows : rows.slice(0, 12);
  });

  constructor() {
    this.api.getRegions().subscribe((regions) => this.regions.set(regions));

    effect(() => {
      const filter = this.filter();
      if (this.rangeError()) {
        return;
      }
      untracked(() => this.load(filter));
    });

    effect(() => {
      const filter = this.filter();
      const subject = this.buyerSubject();
      if (this.rangeError()) {
        return;
      }
      untracked(() => this.loadBuyers(filter, subject));
    });
  }

  setRegion(regionId: number | null): void {
    this.regionId.set(regionId);
    const store = this.catalog.stores().find((item) => item.storeId === this.storeId());
    if (store && regionId !== null && store.regionId !== regionId) {
      this.storeId.set(null);
    }
  }

  resetFilters(): void {
    this.preset.set('all');
    this.customFrom.set('');
    this.customTo.set('');
    this.regionId.set(null);
    this.storeId.set(null);
  }

  readonly money = money;
  readonly count = count;
  readonly money0 = axisMoney;

  private segmentData(rows: SegmentSales[], rename: (name: string) => string = (name) => name): ChartDatum[] {
    return rows.map((row) => ({
      label: rename(row.segment),
      value: Number(row.revenue),
      display: moneyCompact(row.revenue),
      details: [
        { label: 'Share', value: percent(row.revenueSharePercent) },
        { label: 'Customers', value: count(row.customerCount) },
        { label: 'Orders', value: count(row.orderCount) },
        { label: 'Avg order', value: money(row.averageOrderValue) }
      ]
    }));
  }

  private load(filter: ReportFilter): void {
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      overview: this.api.reportOverview(filter),
      monthly: this.api.reportMonthly(filter),
      categories: this.api.reportCategories(filter),
      regions: this.api.reportRegions(filter),
      stores: this.api.reportStores(filter),
      products: this.api.reportProducts(filter),
      salespeople: this.api.reportSalespeople(filter),
      segments: this.api.reportSegments(filter),
      payments: this.api.reportPaymentMethods(filter),
      topCustomers: this.api.reportTopCustomers(filter)
    }).subscribe({
      next: (data) => {
        this.data.set(data);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(errorMessage(error, 'The reports could not be loaded.'));
        this.loading.set(false);
      }
    });
  }

  private loadBuyers(filter: ReportFilter, subject: string): void {
    const [kind, id] = subject.split(':');
    const productId = kind === 'product' ? Number(id) : null;
    const categoryId = kind === 'category' ? Number(id) : null;
    this.buyersLoading.set(true);
    this.api.reportBusinessBuyers(filter, productId, categoryId).subscribe({
      next: (buyers) => {
        this.buyers.set(buyers);
        this.buyersLoading.set(false);
      },
      error: () => this.buyersLoading.set(false)
    });
  }
}
