package com.starbuck.moneytracker.unit.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.starbuck.moneytracker.commands.TransactionDetailSaveCommand;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;

class TransactionDetailSaveCommandTest {

    @Test
    void simplePriceMode_nullPrice_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionDetailSaveCommand("tétel", null, List.of(), TransactionTypeEnum.INCOME));
    }

    @Test
    void simplePriceMode_blankName_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionDetailSaveCommand("  ", new BigDecimal("100"), List.of(),
                        TransactionTypeEnum.INCOME));
    }

    @Test
    void simplePriceMode_incomeWithNonPositivePrice_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionDetailSaveCommand("tétel", BigDecimal.ZERO, List.of(),
                        TransactionTypeEnum.INCOME));
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionDetailSaveCommand("tétel", new BigDecimal("-1"), List.of(),
                        TransactionTypeEnum.INCOME));
    }

    @Test
    void simplePriceMode_outcomeWithNonNegativePrice_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionDetailSaveCommand("tétel", BigDecimal.ZERO, List.of(),
                        TransactionTypeEnum.OUTCOME));
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionDetailSaveCommand("tétel", new BigDecimal("1"), List.of(),
                        TransactionTypeEnum.OUTCOME));
    }

    @Test
    void simplePriceMode_validIncomeDetail_assignsAllFields() {
        var command = new TransactionDetailSaveCommand("tej", new BigDecimal("300"), List.of(1L, 2L),
                TransactionTypeEnum.INCOME);

        assertEquals("tej", command.getName());
        assertEquals(new BigDecimal("300"), command.getPrice());
        assertEquals(List.of(1L, 2L), command.getCategories());
        assertFalse(command.isComplexPriceMode());
    }

    @Test
    void simplePriceMode_validOutcomeDetail_isAccepted() {
        var command = new TransactionDetailSaveCommand("kenyér", new BigDecimal("-300"), List.of(),
                TransactionTypeEnum.OUTCOME);

        assertEquals(new BigDecimal("-300"), command.getPrice());
    }

    @Test
    void complexPriceMode_nullWeight_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionDetailSaveCommand("tétel", null, new BigDecimal("100"), List.of()));
    }

    @Test
    void complexPriceMode_nullUnitPrice_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionDetailSaveCommand("tétel", new BigDecimal("5"), null, List.of()));
    }

    @Test
    void complexPriceMode_nonPositiveWeightOrUnitPrice_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionDetailSaveCommand("tétel", BigDecimal.ZERO, new BigDecimal("100"), List.of()));
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionDetailSaveCommand("tétel", new BigDecimal("5"), BigDecimal.ZERO, List.of()));
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionDetailSaveCommand("tétel", new BigDecimal("-5"), new BigDecimal("100"),
                        List.of()));
    }

    @Test
    void complexPriceMode_blankName_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionDetailSaveCommand(null, new BigDecimal("5"), new BigDecimal("100"), List.of()));
    }

    @Test
    void complexPriceMode_validInput_assignsAllFieldsAndIsComplex() {
        var command = new TransactionDetailSaveCommand("hús", new BigDecimal("0.5"), new BigDecimal("2000"),
                List.of(7L));

        assertEquals("hús", command.getName());
        assertEquals(new BigDecimal("0.5"), command.getWeight());
        assertEquals(new BigDecimal("2000"), command.getUnitPrice());
        assertEquals(List.of(7L), command.getCategories());
        assertTrue(command.isComplexPriceMode());
    }
}
