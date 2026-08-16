package com.starbuck.moneytracker.service.domainservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;

class CostCalculatorDomainServiceTest {

    private CostCalculatorDomainService service;

    @BeforeEach
    void setUp() {
        this.service = new CostCalculatorDomainService();
    }

    @Nested
    @DisplayName("calculateCost - hibás bemenetek")
    class WrongInputs {

        @Test
        void nullDetailAndNullType_throws() {
            assertThrowsExactly(IllegalArgumentException.class, () -> service.calculateCost(null, null));
        }

        @Test
        void nullType_throws() {
            var detail = new TransactionDetail("teszt", new BigDecimal("1000"));
            assertThrowsExactly(IllegalArgumentException.class, () -> service.calculateCost(detail, null));
        }

        @Test
        void emptyDetail_throws() {
            assertThrowsExactly(IllegalArgumentException.class,
                    () -> service.calculateCost(new TransactionDetail(), TransactionTypeEnum.INCOME));
        }

        @Test
        void weightWithoutUnitPriceAndPrice_throws() {
            var detail = new TransactionDetail("teszt", new BigDecimal("5"), null);
            assertThrowsExactly(IllegalArgumentException.class,
                    () -> service.calculateCost(detail, TransactionTypeEnum.INCOME));
        }

        @Test
        void unitPriceWithoutWeightAndPrice_throws() {
            var detail = new TransactionDetail("teszt", null, new BigDecimal("5"));
            assertThrowsExactly(IllegalArgumentException.class,
                    () -> service.calculateCost(detail, TransactionTypeEnum.INCOME));
        }

        @Test
        @DisplayName("Price nem lehet pozitív szám, hogyha outcome a tranzakció típusa")
        void pricePositiveWhenOutcome_throws() {
            var transaction = new Transaction("bevásárlás", null, TransactionTypeEnum.OUTCOME, null);
            var detail = new TransactionDetail("tej", new BigDecimal("300"));
            transaction.setTransactionDetails(Set.of(detail));

            assertThrowsExactly(IllegalArgumentException.class,
                    () -> service.calculateTransactionCost(transaction));

        }
        @Test
        @DisplayName("Price nem lehet negatív szám, hogyha income a tranzakció típusa")
        void priceNegativeWhenIncome_throws() {
            var transaction = new Transaction("bevásárlás", null, TransactionTypeEnum.INCOME, null);
            var detail = new TransactionDetail("Fizetés", new BigDecimal("-300"));
            transaction.setTransactionDetails(Set.of(detail));

            assertThrowsExactly(IllegalArgumentException.class,
                    () -> service.calculateTransactionCost(transaction));

        }
    }

    @Nested
    @DisplayName("calculateCost - weight * unitPrice mód")
    class ComplexPriceMode {

        @Test
        @DisplayName("weight és unitPrice megléte esetén nem a price-t adja vissza")
        void priceHasSmallerPriorityThanWeightAndUnitPrice() {
            var detail = new TransactionDetail("teszt", new BigDecimal("5"), new BigDecimal("1000"));
            detail.setPrice(new BigDecimal("100"));

            assertEquals(new BigDecimal("5000.00"), service.calculateCost(detail, TransactionTypeEnum.INCOME));
            assertEquals(new BigDecimal("-5000.00"), service.calculateCost(detail, TransactionTypeEnum.OUTCOME));
        }

        @Test
        @DisplayName("HALF_UP kerekítés 2 tizedesjegyre")
        void roundsHalfUpToTwoDecimals() {
            var roundsUp = new TransactionDetail("roundsUp", new BigDecimal("3"), new BigDecimal("0.005"));
            assertEquals(new BigDecimal("0.02"), service.calculateCost(roundsUp, TransactionTypeEnum.INCOME));

            var roundsDown = new TransactionDetail("roundsDown", new BigDecimal("1"), new BigDecimal("0.004"));
            assertEquals(new BigDecimal("0.00"), service.calculateCost(roundsDown, TransactionTypeEnum.INCOME));
        }

        @Test
        @DisplayName("nulla weight vagy unitPrice esetén nulla a költség, előjeltől függetlenül")
        void zeroWeightOrUnitPrice_resultsInZero() {
            var detail = new TransactionDetail("zero", BigDecimal.ZERO, new BigDecimal("100"));

            assertEquals(new BigDecimal("0.00"), service.calculateCost(detail, TransactionTypeEnum.INCOME));
            assertEquals(new BigDecimal("0.00"), service.calculateCost(detail, TransactionTypeEnum.OUTCOME));
        }
    }

    @Nested
    @DisplayName("calculateCost - price mód")
    class SimplePriceMode {

        @Test
        @DisplayName("price mód esetén a tranzakció típusa nem módosítja az előjelet, mert az már a price-ban tárolva van")
        void priceIsUsedAsIs_regardlessOfTransactionType() {
            var detail = new TransactionDetail("price", new BigDecimal("1000"));

            assertEquals(new BigDecimal("1000.00"), service.calculateCost(detail, TransactionTypeEnum.INCOME));

            var negativeDetail = new TransactionDetail("negativePrice", new BigDecimal("-500"));
            assertEquals(new BigDecimal("-500.00"), service.calculateCost(negativeDetail, TransactionTypeEnum.OUTCOME));
        }

        @Test
        @DisplayName("price mód esetén is 2 tizedesjegyre kerekít")
        void roundsPriceToTwoDecimals() {
            var detail = new TransactionDetail("price", new BigDecimal("1000.005"));
            assertEquals(new BigDecimal("1000.01"), service.calculateCost(detail, TransactionTypeEnum.INCOME));
        }
    }

    @Nested
    @DisplayName("calculateTransactionCost")
    class TransactionCost {
        @Test
        @DisplayName("összeadja a tranzakció összes tételének költségét")
        void sumsAllDetailCosts() {
            var transaction = new Transaction("bevásárlás", null, TransactionTypeEnum.OUTCOME, null);
            var detail1 = new TransactionDetail("kenyér", new BigDecimal("2"), new BigDecimal("500"));
            var detail2 = new TransactionDetail("tej", new BigDecimal("-300"));
            transaction.setTransactionDetails(Set.of(detail1, detail2));

            var result = service.calculateTransactionCost(transaction);

            assertEquals(new BigDecimal("-1300.00"), result);
        }

        @Test
        @DisplayName("üres detail lista esetén nulla a végösszeg")
        void emptyDetails_resultsInZero() {
            var transaction = new Transaction("üres", null, TransactionTypeEnum.INCOME, null);
            transaction.setTransactionDetails(Set.of());

            assertEquals(BigDecimal.ZERO, service.calculateTransactionCost(transaction));
        }
    }
}
