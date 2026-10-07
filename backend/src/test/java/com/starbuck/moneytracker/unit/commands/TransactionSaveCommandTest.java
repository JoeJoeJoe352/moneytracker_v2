package com.starbuck.moneytracker.unit.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.starbuck.moneytracker.commands.TransactionDetailSaveCommand;
import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;

class TransactionSaveCommandTest {

    @Test
    void nullDetailList_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionSaveCommand("teszt", null, LocalDate.now(), TransactionTypeEnum.INCOME,
                        null, List.of(), 1L));
    }

    @Test
    void allowsEmptyDetailList() {
        var command = new TransactionSaveCommand("teszt", new BigDecimal("10"), LocalDate.now(),
                TransactionTypeEnum.INCOME, List.of(), List.of(), 1L);

        assertEquals(0, command.getDetailCommands().size());
    }

    @Test
    void nullName_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionSaveCommand(null, new BigDecimal("300"), LocalDate.now(), TransactionTypeEnum.INCOME,
                        List.of(), List.of(), 1L));
    }

    @Test
    void blankName_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionSaveCommand("   ", new BigDecimal("300"), LocalDate.now(), TransactionTypeEnum.INCOME,
                        List.of(), List.of(), 1L));
    }

    @Test
    void nullDate_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionSaveCommand("teszt", new BigDecimal("300"), null, TransactionTypeEnum.INCOME,
                        List.of(), List.of(), 1L));
    }

    @Test
    void nullType_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionSaveCommand("teszt", new BigDecimal("300"), LocalDate.now(), null,
                        List.of(), List.of(), 1L));
    }
    @Test
    void noGlobalPriceAndNoDetails_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionSaveCommand("teszt", null, LocalDate.now(), TransactionTypeEnum.INCOME,
                        List.of(), List.of(), 1L));
    }
    @Test
    void globalPriceAndDetails_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionSaveCommand("teszt", new BigDecimal("300"), LocalDate.now(), TransactionTypeEnum.INCOME,
                        List.of(new TransactionDetailSaveCommand("teszt", new BigDecimal("400"), List.of(), TransactionTypeEnum.INCOME)), List.of(), 1L));
    }

    @Test
    void validInput_assignsAllFields() {
        TransactionDetailSaveCommand detail = new TransactionDetailSaveCommand("tétel", new BigDecimal("100"),
                List.of(), TransactionTypeEnum.INCOME);
        LocalDate date = LocalDate.of(2026, 3, 1);

        var command = new TransactionSaveCommand("teszt", null, date,
                TransactionTypeEnum.INCOME, List.of(detail), List.of(1L, 2L), 1L);

        assertEquals("teszt", command.getTransactionName());
        assertEquals(null, command.getGlobalPrice());
        assertEquals(date, command.getTransactionDate());
        assertEquals(TransactionTypeEnum.INCOME, command.getTransactionType());
        assertEquals(List.of(detail), command.getDetailCommands());
        assertEquals(List.of(1L, 2L), command.getCategories());
    }
}
