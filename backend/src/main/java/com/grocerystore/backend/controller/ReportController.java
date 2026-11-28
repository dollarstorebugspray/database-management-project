package com.grocerystore.backend.controller;

import java.time.LocalDate;
import java.util.List;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.grocerystore.backend.dto.ReportFilter;
import com.grocerystore.backend.dto.Reports.BusinessBuyers;
import com.grocerystore.backend.dto.Reports.CategorySales;
import com.grocerystore.backend.dto.Reports.CustomerSegments;
import com.grocerystore.backend.dto.Reports.MonthlySales;
import com.grocerystore.backend.dto.Reports.Overview;
import com.grocerystore.backend.dto.Reports.PaymentMethodSales;
import com.grocerystore.backend.dto.Reports.ProductSales;
import com.grocerystore.backend.dto.Reports.RegionSales;
import com.grocerystore.backend.dto.Reports.SalespersonSales;
import com.grocerystore.backend.dto.Reports.StoreSales;
import com.grocerystore.backend.dto.Reports.TopCustomer;
import com.grocerystore.backend.service.AuthService;
import com.grocerystore.backend.service.ReportService;

@Validated
@RestController
@RequestMapping("/api/admin/reports")
public class ReportController {

    private final ReportService reportService;
    private final AuthService authService;

    public ReportController(ReportService reportService, AuthService authService) {
        this.reportService = reportService;
        this.authService = authService;
    }

    @GetMapping("/overview")
    public Overview overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Min(1) Integer storeId,
            @RequestParam(required = false) @Min(1) Integer regionId,
            HttpSession session) {
        authService.requireStaff(session);
        return reportService.overview(new ReportFilter(from, to, storeId, regionId));
    }

    // 1. Product sales, revenue, and gross profit
    @GetMapping("/products")
    public List<ProductSales> products(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Min(1) Integer storeId,
            @RequestParam(required = false) @Min(1) Integer regionId,
            @RequestParam(required = false) @Min(1) Integer categoryId,
            @RequestParam(required = false) @Min(1) @Max(500) Integer limit,
            HttpSession session) {
        authService.requireStaff(session);
        return reportService.products(new ReportFilter(from, to, storeId, regionId), categoryId, limit);
    }

    // 2. Top product categories
    @GetMapping("/categories")
    public List<CategorySales> categories(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Min(1) Integer storeId,
            @RequestParam(required = false) @Min(1) Integer regionId,
            HttpSession session) {
        authService.requireStaff(session);
        return reportService.categories(new ReportFilter(from, to, storeId, regionId));
    }

    // 3. Regional sales comparison
    @GetMapping("/regions")
    public List<RegionSales> regions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Min(1) Integer storeId,
            @RequestParam(required = false) @Min(1) Integer regionId,
            HttpSession session) {
        authService.requireStaff(session);
        return reportService.regions(new ReportFilter(from, to, storeId, regionId));
    }

    @GetMapping("/stores")
    public List<StoreSales> stores(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Min(1) Integer storeId,
            @RequestParam(required = false) @Min(1) Integer regionId,
            HttpSession session) {
        authService.requireStaff(session);
        return reportService.stores(new ReportFilter(from, to, storeId, regionId));
    }

    // 4. Businesses purchasing a given product
    @GetMapping("/business-buyers")
    public BusinessBuyers businessBuyers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Min(1) Integer storeId,
            @RequestParam(required = false) @Min(1) Integer regionId,
            @RequestParam(required = false) @Min(1) Integer productId,
            @RequestParam(required = false) @Min(1) Integer categoryId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer limit,
            HttpSession session) {
        authService.requireStaff(session);
        return reportService.businessBuyers(
                new ReportFilter(from, to, storeId, regionId), productId, categoryId, limit);
    }

    @GetMapping("/salespeople")
    public List<SalespersonSales> salespeople(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Min(1) Integer storeId,
            @RequestParam(required = false) @Min(1) Integer regionId,
            HttpSession session) {
        authService.requireStaff(session);
        return reportService.salespeople(new ReportFilter(from, to, storeId, regionId));
    }

    @GetMapping("/monthly")
    public List<MonthlySales> monthly(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Min(1) Integer storeId,
            @RequestParam(required = false) @Min(1) Integer regionId,
            HttpSession session) {
        authService.requireStaff(session);
        return reportService.monthly(new ReportFilter(from, to, storeId, regionId));
    }

    @GetMapping("/customer-segments")
    public CustomerSegments customerSegments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Min(1) Integer storeId,
            @RequestParam(required = false) @Min(1) Integer regionId,
            HttpSession session) {
        authService.requireStaff(session);
        return reportService.customerSegments(new ReportFilter(from, to, storeId, regionId));
    }

    @GetMapping("/top-customers")
    public List<TopCustomer> topCustomers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Min(1) Integer storeId,
            @RequestParam(required = false) @Min(1) Integer regionId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer limit,
            HttpSession session) {
        authService.requireStaff(session);
        return reportService.topCustomers(new ReportFilter(from, to, storeId, regionId), limit);
    }

    @GetMapping("/payment-methods")
    public List<PaymentMethodSales> paymentMethods(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Min(1) Integer storeId,
            @RequestParam(required = false) @Min(1) Integer regionId,
            HttpSession session) {
        authService.requireStaff(session);
        return reportService.paymentMethods(new ReportFilter(from, to, storeId, regionId));
    }

}
