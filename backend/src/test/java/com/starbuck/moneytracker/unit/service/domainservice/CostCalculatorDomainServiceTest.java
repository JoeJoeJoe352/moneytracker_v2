package com.starbuck.moneytracker.unit.service.domainservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.starbuck.moneytracker.commands.TransactionCreateCommand;
import com.starbuck.moneytracker.commands.TransactionDetailSaveCommand;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;
import com.starbuck.moneytracker.service.domainservice.CostCalculatorDomainService;

class CostCalculatorDomainServiceTest {

    private CostCalculatorDomainService service;

    @BeforeEach
    void setUp() {
        this.service = new CostCalculatorDomainService();
    }

    @Test
    void nullDetailAndNullType_throws() {
        assertThrowsExactly(IllegalArgumentException.class, () -> service.calculateCost(null, null));
    }

    @Test
    void nullType_throws() {
        var detail = new TransactionDetailSaveCommand("teszt", new BigDecimal("1000"), List.of(),
                TransactionTypeEnum.INCOME);
        assertThrowsExactly(IllegalArgumentException.class, () -> service.calculateCost(detail, null));
    }

    /**
     * Price nem lehet pozitív szám, hogyha outcome a tranzakció típusa
     */
    @Test
    void pricePositiveWhenOutcome_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionDetailSaveCommand("tej", new BigDecimal("300"), List.of(),
                        TransactionTypeEnum.OUTCOME));
    }

    /**
     * Price nem lehet negatív szám, hogyha income a tranzakció típusa
     */
    @Test
    void priceNegativeWhenIncome_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionDetailSaveCommand("Fizetés", new BigDecimal("-300"), List.of(),
                        TransactionTypeEnum.INCOME));
    }

    @Test
    void simpleModeWithNullPrice_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionDetailSaveCommand("teszt", null, List.of(), TransactionTypeEnum.INCOME));
    }

    @Test
    void complexModeWithNullUnitPrice_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionDetailSaveCommand("teszt", new BigDecimal("5"), null, List.of()));
    }

    @Test
    void complexModeWithNullWeight_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionDetailSaveCommand("teszt", null, new BigDecimal("5"), List.of()));
    }

    /**
     * Üres detail listára nullát ad vissza
     */
    @Test
    void emptyDetailArray_returnsZero() {
        var transaction = new TransactionCreateCommand("bevásárlás", new BigDecimal("300.00"), LocalDate.now(),
                TransactionTypeEnum.INCOME, List.of(), List.of(), 1L);

        assertEquals(new BigDecimal("300.00"), service.calculateTransactionCost(transaction));
    }

    /**
     * weight és unitPrice megléte esetén nem a price-t adja vissza
     */
    @Test
    void priceHasSmallerPriorityThanWeightAndUnitPrice() {
        var detail = new TransactionDetailSaveCommand("teszt", new BigDecimal("5"), new BigDecimal("1000"),
                List.of());

        assertEquals(new BigDecimal("5000.00"), service.calculateCost(detail, TransactionTypeEnum.INCOME));
        assertEquals(new BigDecimal("-5000.00"), service.calculateCost(detail, TransactionTypeEnum.OUTCOME));
    }

    /**
     * HALF_UP kerekítés 2 tizedesjegyre, weight * unitPrice mód esetén
     */
    @Test
    void roundsHalfUpToTwoDecimals() {
        var roundsUp = new TransactionDetailSaveCommand("roundsUp", new BigDecimal("3"), new BigDecimal("0.005"),
                List.of());
        assertEquals(new BigDecimal("0.02"), service.calculateCost(roundsUp, TransactionTypeEnum.INCOME));

        var roundsDown = new TransactionDetailSaveCommand("roundsDown", new BigDecimal("1"),
                new BigDecimal("0.004"), List.of());
        assertEquals(new BigDecimal("0.00"), service.calculateCost(roundsDown, TransactionTypeEnum.INCOME));
    }

    /**
     * price mód esetén a tranzakció típusa nem módosítja az előjelet, mert az már a price-ban tárolva van
     */
    @Test
    void priceIsUsedAsIs_regardlessOfTransactionType() {
        var detail = new TransactionDetailSaveCommand("price", new BigDecimal("1000"), List.of(),
                TransactionTypeEnum.INCOME);

        assertEquals(new BigDecimal("1000.00"), service.calculateCost(detail, TransactionTypeEnum.INCOME));

        var negativeDetail = new TransactionDetailSaveCommand("negativePrice", new BigDecimal("-500"),
                List.of(), TransactionTypeEnum.OUTCOME);
        assertEquals(new BigDecimal("-500.00"), service.calculateCost(negativeDetail, TransactionTypeEnum.OUTCOME));
    }

    /**
     * price mód esetén is 2 tizedesjegyre kerekít
     */
    @Test
    void roundsPriceToTwoDecimals() {
        var detail = new TransactionDetailSaveCommand("price", new BigDecimal("1000.005"), List.of(),
                TransactionTypeEnum.INCOME);
        assertEquals(new BigDecimal("1000.01"), service.calculateCost(detail, TransactionTypeEnum.INCOME));
    }

    /**
     * calculateTransactionCost összeadja a tranzakció összes tételének költségét
     */
    @Test
    void sumsAllDetailCosts() {
        var detail1 = new TransactionDetailSaveCommand("kenyér", new BigDecimal("2"), new BigDecimal("500"),
                List.of());
        var detail2 = new TransactionDetailSaveCommand("tej", new BigDecimal("-300"), List.of(),
                TransactionTypeEnum.OUTCOME);
        var transaction = new TransactionCreateCommand("bevásárlás", null, LocalDate.now(),
                TransactionTypeEnum.OUTCOME, List.of(detail1, detail2), List.of(), 1L);

        var result = service.calculateTransactionCost(transaction);

        assertEquals(new BigDecimal("-1300.00"), result);
    }
}
