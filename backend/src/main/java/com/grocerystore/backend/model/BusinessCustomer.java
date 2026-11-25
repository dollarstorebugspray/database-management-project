
package com.grocerystore.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "BusinessCustomer")
public class BusinessCustomer {

    @Id
    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "business_category")
    private String businessCategory;

    @Column(name = "annual_income")
    private BigDecimal annualIncome;

    public BusinessCustomer() {
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getBusinessCategory() {
        return businessCategory;
    }

    public void setBusinessCategory(String businessCategory) {
        this.businessCategory = businessCategory;
    }

    public BigDecimal getAnnualIncome() {
        return annualIncome;
    }

    public void setAnnualIncome(BigDecimal annualIncome) {
        this.annualIncome = annualIncome;
    }
}
