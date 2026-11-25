
package com.grocerystore.backend.controller;

import com.grocerystore.backend.repository.SalesReportRepository;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class SalesReportController {

    private final SalesReportRepository salesReportRepository;

    public SalesReportController(
            SalesReportRepository salesReportRepository) {

        this.salesReportRepository = salesReportRepository;
    }

    // 1. Product sales, revenue, and gross profit
    @GetMapping("/products")
    public List<Map<String, Object>> getProductSales() {

        List<Map<String, Object>> reports = new ArrayList<>();

        for (Object[] row : salesReportRepository.getProductSales()) {

            Map<String, Object> report = new LinkedHashMap<>();

            report.put("productId", row[0]);
            report.put("productName", row[1]);
            report.put("totalQuantitySold", row[2]);
            report.put("totalRevenue", row[3]);
            report.put("totalGrossProfit", row[4]);
            report.put("salesMissingCost", row[5]);

            reports.add(report);
        }

        return reports;
    }

    // 2. Top product categories
    @GetMapping("/categories")
    public List<Map<String, Object>> getCategorySales() {

        List<Map<String, Object>> reports = new ArrayList<>();

        for (Object[] row : salesReportRepository.getCategorySales()) {

            Map<String, Object> report = new LinkedHashMap<>();

            report.put("categoryId", row[0]);
            report.put("categoryName", row[1]);
            report.put("totalQuantitySold", row[2]);
            report.put("totalRevenue", row[3]);

            reports.add(report);
        }

        return reports;
    }

    // 3. Regional sales comparison
    @GetMapping("/regions")
    public List<Map<String, Object>> getRegionSales() {

        List<Map<String, Object>> reports = new ArrayList<>();

        for (Object[] row : salesReportRepository.getRegionSales()) {

            Map<String, Object> report = new LinkedHashMap<>();

            report.put("regionId", row[0]);
            report.put("regionName", row[1]);
            report.put("totalOrders", row[2]);
            report.put("totalQuantitySold", row[3]);
            report.put("totalRevenue", row[4]);

            reports.add(report);
        }

        return reports;
    }

    // 4. Businesses purchasing a given product
    @GetMapping("/businesses")
    public List<Map<String, Object>> getBusinessPurchases(
            @RequestParam Integer productId) {

        List<Map<String, Object>> reports = new ArrayList<>();

        for (Object[] row :
                salesReportRepository.getBusinessPurchases(productId)) {

            Map<String, Object> report = new LinkedHashMap<>();

            report.put("customerId", row[0]);
            report.put("businessName", row[1]);
            report.put("productId", row[2]);
            report.put("productName", row[3]);
            report.put("totalQuantityPurchased", row[4]);
            report.put("totalAmountSpent", row[5]);

            reports.add(report);
        }

        return reports;
    }
}
