package com.starbuck.moneytracker.commands;

import java.math.BigDecimal;
import java.util.List;

import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;
import com.starbuck.moneytracker.types.WeightedPrice;

/**
 * Tranzakció detail mentéshez való command osztály, kiszűri a hibás
 * adatvariánsokat
 */
public class TransactionDetailSaveCommand {

    /**
     * Tétel neve
     */
    String name;
    /**
     * Tétel ára
     * Lehet null, ha súlyozott árat adunk meg
     */
    BigDecimal price;
    /**
     * Tétel kategória azonosítói
     */
    List<Long> categories;
    /**
     * Súlyból és egységárból számolt ár
     * lehet null, ha árat adunk meg
     */
    WeightedPrice weightedPrice;

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

        this.setName(name);
        this.weightedPrice = new WeightedPrice(weight, unitPrice);
        this.categories = categories;
    }

    /**
     * Összetett-e a detail árképzése
     * 
     * @return
     */
    public boolean isComplexPriceMode() {
        return this.weightedPrice != null;
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
        return this.weightedPrice == null ? null : this.weightedPrice.getWeight();
    }

    public BigDecimal getUnitPrice() {
        return this.weightedPrice == null ? null : this.weightedPrice.getUnitPrice();
    }

    public List<Long> getCategories() {
        return categories;
    }

}
