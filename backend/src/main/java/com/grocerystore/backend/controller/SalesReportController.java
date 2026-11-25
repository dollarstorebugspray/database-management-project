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

    // Retrieve sales statistics for each product
    @GetMapping("/products")
    public List<Map<String, Object>> getProductSales() {

        List<Object[]> results =
                salesReportRepository.getProductSales();

        List<Map<String, Object>> reports = new ArrayList<>();

        for (Object[] row : results) {

            Map<String, Object> report = new LinkedHashMap<>();

            report.put("productId", row[0]);
            report.put("productName", row[1]);
            report.put("totalQuantitySold", row[2]);
            report.put("totalRevenue", row[3]);

            reports.add(report);
        }

        return reports;
    }
}
