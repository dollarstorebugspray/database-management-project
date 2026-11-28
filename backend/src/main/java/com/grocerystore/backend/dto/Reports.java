package com.grocerystore.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class Reports {

    private Reports() {
    }

    public record Overview(
            BigDecimal revenue,
            BigDecimal collected,
            BigDecimal profit,
            long orderCount,
            long unitsSold,
            BigDecimal averageOrderValue,
            long customerCount,
            long cancelledOrders) {
    }

    public record ProductSales(
            Integer productId,
            String productName,
            String brand,
            String categoryName,
            long unitsSold,
            long orderCount,
            BigDecimal revenue,
            BigDecimal profit) {
    }

    public record CategorySales(
            Integer categoryId,
            String categoryName,
            long productCount,
            long unitsSold,
            long orderCount,
            BigDecimal revenue,
            BigDecimal profit,
            BigDecimal revenueSharePercent) {
    }

    public record RegionSales(
            Integer regionId,
            String regionName,
            String regionManager,
            long storeCount,
            long orderCount,
            long unitsSold,
            BigDecimal revenue,
            BigDecimal profit,
            BigDecimal averageOrderValue,
            BigDecimal revenueSharePercent) {
    }

    public record StoreSales(
            Integer storeId,
            String street,
            String city,
            String regionName,
            String manager,
            long salespersonCount,
            long orderCount,
            long unitsSold,
            BigDecimal revenue,
            BigDecimal profit) {
    }

    public record BusinessBuyer(
            Integer customerId,
            String name,
            String businessCategory,
            String city,
            long orderCount,
            long unitsBought,
            BigDecimal totalSpent,
            LocalDate lastOrderDate) {
    }

    public record BusinessCategoryPurchase(
            String businessCategory,
            long customerCount,
            long unitsBought,
            BigDecimal totalSpent) {
    }

    public record BusinessBuyers(
            String subject,
            List<BusinessBuyer> buyers,
            List<BusinessCategoryPurchase> byBusinessCategory) {
    }

    public record SalespersonSales(
            Integer salespersonId,
            String name,
            String jobTitle,
            Integer storeId,
            String storeCity,
            long orderCount,
            long unitsSold,
            BigDecimal revenue,
            BigDecimal averageOrderValue) {
    }

    public record MonthlySales(
            String month,
            long orderCount,
            long unitsSold,
            BigDecimal revenue,
            BigDecimal profit) {
    }

    public record SegmentSales(
            String segment,
            long customerCount,
            long orderCount,
            BigDecimal revenue,
            BigDecimal averageOrderValue,
            BigDecimal revenueSharePercent) {
    }

    public record CustomerSegments(
            List<SegmentSales> customerTypes,
            List<SegmentSales> ageBands,
            List<SegmentSales> incomeBands) {
    }

    public record TopCustomer(
            Integer customerId,
            String name,
            String customerType,
            long orderCount,
            long unitsBought,
            BigDecimal totalSpent,
            LocalDate lastOrderDate) {
    }

    public record PaymentMethodSales(
            String paymentMethod,
            long orderCount,
            BigDecimal revenue,
            BigDecimal revenueSharePercent) {
    }
}
