package com.starbuck.moneytracker.entity;

import java.math.BigDecimal;

/**
 * Tranzakció típusa.
 *
 * Strategy mintát valósít meg: minden érték maga tudja, hogyan kell
 * előjelet váltania egy összegen, és milyen szabály szerint érvényes egy
 * TransactionDetail ára. Így elkerülhető az isOutcome/type szerinti
 * if-else ágazás a service rétegben.
 */
public enum TransactionTypeEnum {
    INCOME {
        @Override
        public BigDecimal applySign(BigDecimal amount) {
            return amount;
        }

        @Override
        public void validateDetailPrice(BigDecimal price) {
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Income transaction, with a negative or zero detail!");
            }
        }
    },
    OUTCOME {
        @Override
        public BigDecimal applySign(BigDecimal amount) {
            return amount.negate();
        }

        @Override
        public void validateDetailPrice(BigDecimal price) {
            if (price.compareTo(BigDecimal.ZERO) >= 0) {
                throw new IllegalArgumentException(
                        "Expense transaction, with a positive or zero detail!");
            }
        }
    };

    /**
     * Ráalkalmazza a típusnak megfelelő előjelet egy price-ra.
     */
    public abstract BigDecimal applySign(BigDecimal amount);

    /**
     * Ellenőrzi, hogy a detail ára megfelel-e a tranzakció típusának
     * (income csak pozitív, outcome csak negatív lehet).
     *
     * @throws IllegalArgumentException ha az ár nem megfelelő előjelű
     */
    public abstract void validateDetailPrice(BigDecimal price);
}
