package com.starbuck.moneytracker.service.domainservice;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.starbuck.moneytracker.commands.TransactionDetailSaveCommand;
import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;

public class CostCalculatorDomainService {

    /**
     * Kiszámolja egy TransactionDetail költségét.
     * - Ha weight + unitPrice meg van adva → kiszámolja, majd a tranzakció típusára
     * (income/outcome) bízza az előjel eldöntését
     * 
     * - Ha nincs akkor a price-t próbálja meg visszaadni
     * - Minden eredmény scale=2, HALF_UP
     */
    public BigDecimal calculateCost(TransactionDetailSaveCommand command, TransactionTypeEnum type) {

        if (command == null) {
            throw new IllegalArgumentException("detail cannot be null");
        }

        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

        if (command.isComplexPriceMode()) {
            BigDecimal cost = command.getWeight()
                    .multiply(command.getUnitPrice())
                    .setScale(2, RoundingMode.HALF_UP);

            return type.applySign(cost);
        }

        // Db-ben két tizedesjegyre lesz szűkítve, de ha nem volt még mentve, akkor
        // jobb, ha forceoljuk a két tizedesjegyet
        return command.getPrice().setScale(2, RoundingMode.HALF_UP);

    }

    /**
     * Adott tranzakció összes detail-jához kiszámolja az áraikat és össze is adja
     * őket
     */
    public BigDecimal calculateTransactionCost(TransactionSaveCommand command) {
        return command.getDetailCommands().stream()
                .map((detail) -> this.calculateCost(detail, command.getTransactionType()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
