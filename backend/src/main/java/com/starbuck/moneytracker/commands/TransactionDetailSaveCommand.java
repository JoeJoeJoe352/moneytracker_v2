package com.starbuck.moneytracker.commands;

import java.math.BigDecimal;
import java.util.List;

import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;

/**
 * Tranzakció detail mentéshez való command osztály, kiszűri a hibás adatvariánsokat
 */
public class TransactionDetailSaveCommand {

    /**
     * Tétel neve
     */
    String name;
    /**
     * Tétel ára
     * Lehet null, ha súly és egységárat adunk meg
     */
    BigDecimal price;
    /**
     * Tétel súlya
     * Lehet null, ha árat adunk meg
     */
    BigDecimal weight;
    /**
     * Tétel egységára
     * Lehet null, ha árat adunk meg
     */
    BigDecimal unitPrice;
    /**
     * Tétel kategória azonosítói
     */
    List<Long> categories;

    /**
     * Ha egyszerű áras a detail
     * 
     * @param name
     * @param price
     * @param categories
     * @param transactionType
     */
    public TransactionDetailSaveCommand(String name, BigDecimal price,
            List<Long> categories, TransactionTypeEnum transactionType) {

        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
        transactionType.validateDetailPrice(price);

        this.setName(name);
        this.name = name;
        this.price = price;
        this.categories = categories;
    }

    /**
     * Ha összetett áras a detail
     * 
     * @param name
     * @param weight
     * @param unitPrice
     * @param categories
     */
    public TransactionDetailSaveCommand(String name, BigDecimal weight, BigDecimal unitPrice,
            List<Long> categories) {
// TODO weightedprice legyen BigDecimal weight, BigDecimal unitPrice helyett
        if (weight == null || unitPrice == null) {
            throw new IllegalArgumentException("Weight and unitprice both required, when one of them is set");
        }
        if (weight.compareTo(BigDecimal.ZERO) < 1 || unitPrice.compareTo(BigDecimal.ZERO) < 1) {
            throw new IllegalArgumentException("Weight and unitprice must be a positive number");
        }

        this.setName(name);
        this.weight = weight;
        this.unitPrice = unitPrice;
        this.categories = categories;
    }

    /**
     * Összetett-e a detail árképzése
     * 
     * @return
     */
    public boolean isComplexPriceMode() {
        return this.weight != null && this.unitPrice != null;
    }

    /**
     * Név validálása és beállítása
     * 
     * @param name
     */
    private void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null.");
        }
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public List<Long> getCategories() {
        return categories;
    }

}
