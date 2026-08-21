package com.starbuck.moneytracker.types;

import java.math.BigDecimal;

/**
 * Súlyból és egységárból számolt ár
 */
public class WeightedPrice {
    private BigDecimal weight;
    private BigDecimal unitPrice;

    public WeightedPrice(BigDecimal weight, BigDecimal unitPrice) {
        if (weight == null || unitPrice == null) {
            throw new IllegalArgumentException("Weight and unitprice required");
        }
        if (weight.compareTo(BigDecimal.ZERO) < 1 || unitPrice.compareTo(BigDecimal.ZERO) < 1) {
            throw new IllegalArgumentException("Weight and unitprice must be a positive number");
        }
        this.weight = weight;
        this.unitPrice = unitPrice;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

}
