package com.starbuck.moneytracker.service.domainservice;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;

public class CostCalculatorDomainService {

    /**
     * Kiszámolja egy TransactionDetail költségét.
     * - Ha weight + unitPrice meg van adva → kiszámolja, majd a tranzakció
     * típusára (income/outcome) bízza az előjel eldöntését
     * - Ha nincs akkor a price-t próbálja meg visszaadni
     * - Minden eredmény scale=2, HALF_UP
     */
    public BigDecimal calculateCost(TransactionDetail detail, TransactionTypeEnum type) {

        if (detail.getWeight() != null && detail.getUnitPrice() != null) {
            BigDecimal cost = detail.getWeight()
                    .multiply(detail.getUnitPrice())
                    .setScale(2, RoundingMode.HALF_UP);

            return type.applySign(cost);
        }

        if (detail.getPrice() != null) {
            // Db-ben két tizedesjegyre lesz szűkítve, de ha nem volt még mentve, akkor
            // jobb, ha forceoljuk a két tizedesjegyet
            return detail.getPrice().setScale(2, RoundingMode.HALF_UP);
        }

        throw new IllegalStateException(
                "Price is null and weight or unitPrice is not defined for TransactionDetail with ID: "
                        + detail.getId());
    }

    /**
     * Adott tranzakció összes detail-jához kiszámolja az áraikat és össze is adja
     * őket
     *
     * @param transaction
     * @return
     */
    public BigDecimal calculateTransactionCost(Transaction transaction) {
        return transaction.getTransactionDetails().stream()
                .map((detail) -> this.calculateCost(detail, transaction.getTransactionType()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
