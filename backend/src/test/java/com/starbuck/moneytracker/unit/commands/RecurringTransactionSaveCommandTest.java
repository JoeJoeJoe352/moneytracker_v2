package com.starbuck.moneytracker.unit.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.starbuck.moneytracker.commands.RecurringTransactionSaveCommand;
import com.starbuck.moneytracker.entity.enums.RecurrenceIntervalEnum;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;

class RecurringTransactionSaveCommandTest {

    @Test
    void nullName_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new RecurringTransactionSaveCommand(
                        null,
                        new BigDecimal("300"),
                        LocalDate.now(),
                        RecurrenceIntervalEnum.MONTHLY,
                        TransactionTypeEnum.INCOME,
                        List.of(),
                        1L));
    }

    @Test
    void blankName_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new RecurringTransactionSaveCommand(
                        "   ",
                        new BigDecimal("300"),
                        LocalDate.now(),
                        RecurrenceIntervalEnum.MONTHLY,
                        TransactionTypeEnum.INCOME,
                        List.of(),
                        1L));
    }

    @Test
    void nullPriceSum_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new RecurringTransactionSaveCommand(
                        "teszt",
                        null,
                        LocalDate.now(),
                        RecurrenceIntervalEnum.MONTHLY,
                        TransactionTypeEnum.INCOME,
                        List.of(),
                        1L));
    }

    @Test
    void nullStartDate_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new RecurringTransactionSaveCommand(
                        "teszt",
                        new BigDecimal("300"),
                        null,
                        RecurrenceIntervalEnum.MONTHLY,
                        TransactionTypeEnum.INCOME,
                        List.of(),
                        1L));
    }

    @Test
    void nullRecurrenceInterval_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new RecurringTransactionSaveCommand(
                        "teszt",
                        new BigDecimal("300"),
                        LocalDate.now(),
                        null,
                        TransactionTypeEnum.INCOME,
                        List.of(),
                        1L));
    }

    @Test
    void nullTransactionType_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new RecurringTransactionSaveCommand(
                        "teszt",
                        new BigDecimal("300"),
                        LocalDate.now(),
                        RecurrenceIntervalEnum.MONTHLY,
                        null,
                        List.of(),
                        1L));
    }

    @Test
    void incomeWithNegativePrice_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new RecurringTransactionSaveCommand(
                        "teszt",
                        new BigDecimal("-300"),
                        LocalDate.now(),
                        RecurrenceIntervalEnum.MONTHLY,
                        TransactionTypeEnum.INCOME,
                        List.of(),
                        1L));
    }

    @Test
    void outcomeWithPositivePrice_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new RecurringTransactionSaveCommand(
                        "teszt",
                        new BigDecimal("300"),
                        LocalDate.now(),
                        RecurrenceIntervalEnum.MONTHLY,
                        TransactionTypeEnum.OUTCOME,
                        List.of(),
                        1L));
    }

    @Test
    void zeroPrice_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new RecurringTransactionSaveCommand(
                        "teszt",
                        BigDecimal.ZERO,
                        LocalDate.now(),
                        RecurrenceIntervalEnum.MONTHLY,
                        TransactionTypeEnum.INCOME,
                        List.of(),
                        1L));
    }

    @Test
    void nullCategoryIds_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new RecurringTransactionSaveCommand(
                        "teszt",
                        new BigDecimal("300"),
                        LocalDate.now(),
                        RecurrenceIntervalEnum.MONTHLY,
                        TransactionTypeEnum.INCOME,
                        null,
                        1L));
    }

    @Test
    void duplicatedCategoryIds_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new RecurringTransactionSaveCommand(
                        "teszt",
                        new BigDecimal("300"),
                        LocalDate.now(),
                        RecurrenceIntervalEnum.MONTHLY,
                        TransactionTypeEnum.INCOME,
                        List.of(1L, 1L),
                        1L));
    }

    @Test
    void nullWalletId_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new RecurringTransactionSaveCommand(
                        "teszt",
                        new BigDecimal("300"),
                        LocalDate.now(),
                        RecurrenceIntervalEnum.MONTHLY,
                        TransactionTypeEnum.INCOME,
                        List.of(),
                        null));
    }

    @Test
    void allowsEmptyCategoryIds() {
        var command = new RecurringTransactionSaveCommand(
                "teszt",
                new BigDecimal("300"),
                LocalDate.now(),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.INCOME,
                List.of(),
                1L);

        assertEquals(0, command.getCategoryIds().size());
    }

    @Test
    void validInput_assignsAllFields() {
        LocalDate date = LocalDate.of(2026, 11, 1);

        var command = new RecurringTransactionSaveCommand(
                "teszt",
                new BigDecimal("-300"),
                date,
                RecurrenceIntervalEnum.WEEKLY,
                TransactionTypeEnum.OUTCOME,
                List.of(1L, 2L),
                5L);

        assertEquals("teszt", command.getName());
        assertEquals(new BigDecimal("-300"), command.getPriceSum());
        assertEquals(date, command.getStartDate());
        assertEquals(RecurrenceIntervalEnum.WEEKLY, command.getRecurrenceInterval());
        assertEquals(TransactionTypeEnum.OUTCOME, command.getTransactionType());
        assertEquals(List.of(1L, 2L), command.getCategoryIds());
        assertEquals(5L, command.getWalletId());
    }
}
