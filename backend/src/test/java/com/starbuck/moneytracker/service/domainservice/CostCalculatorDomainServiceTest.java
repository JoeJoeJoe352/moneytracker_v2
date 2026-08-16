package com.starbuck.moneytracker.service.domainservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;

public class CostCalculatorDomainServiceTest {

    private CostCalculatorDomainService service;

    public CostCalculatorDomainServiceTest() {
        this.service = new CostCalculatorDomainService();
    }

    /**
     * Hibás működések tesztelése
     */
    @Test
    void testCalculateCost_wrongInputs() {
        // null inputs
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            this.service.calculateCost(null, null);
        });

        // empty detail
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            this.service.calculateCost(new TransactionDetail(), TransactionTypeEnum.INCOME);
        });

        // missing unitPrice or weight
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            this.service.calculateCost(new TransactionDetail("teszt", new BigDecimal(5), null),
                    TransactionTypeEnum.INCOME);
        });

        assertThrowsExactly(IllegalArgumentException.class, () -> {
            this.service.calculateCost(new TransactionDetail("teszt", null, new BigDecimal(5)),
                    TransactionTypeEnum.INCOME);
        });
    }

    /**
     * Annak a tesztje, hogy ha unitprice és weight van megadva, akkor újraszámolja
     * az árat, nem a price-t használja
     */
    @Test
    void testCalculateCost_priceHasSmallerPriorityThanWeightAndUnitPrice() {
        var detail = new TransactionDetail("priceHasSmallerPriorityThanWeightAndUnitPrice", new BigDecimal("5"),
                new BigDecimal("1000"));
        detail.setPrice(new BigDecimal("100"));

        var result = this.service.calculateCost(detail, TransactionTypeEnum.INCOME);

        assertEquals(new BigDecimal("5000.00"), result);

        var resultNegative = this.service.calculateCost(detail, TransactionTypeEnum.OUTCOME);

        assertEquals(new BigDecimal("-5000.00"), resultNegative);
    }

    /**
     * Price értékének tesztje
     */
    @Test
    void testCalculateCost_price() {
        var detail = new TransactionDetail("priceHasSmallerPriorityThanWeightAndUnitPrice", new BigDecimal("1000"));

        var result = this.service.calculateCost(detail, TransactionTypeEnum.INCOME);
        assertEquals(new BigDecimal("1000.00"), result);

        // price esetén nincs jelentősége az előjelnek, mert előjelesen van tárolva
        var resultNegative = this.service.calculateCost(detail, TransactionTypeEnum.INCOME);
        assertEquals(new BigDecimal("1000.00"), resultNegative);
    }


}
