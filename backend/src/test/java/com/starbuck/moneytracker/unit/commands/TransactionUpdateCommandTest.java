package com.starbuck.moneytracker.unit.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.starbuck.moneytracker.commands.TransactionDetailSaveCommand;
import com.starbuck.moneytracker.commands.TransactionUpdateCommand;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;

class TransactionUpdateCommandTest {

    private final TransactionDetailSaveCommand detail = new TransactionDetailSaveCommand("tétel",
            new BigDecimal("100"), List.of(), TransactionTypeEnum.INCOME);

    @Test
    void nullDetailList_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionUpdateCommand("teszt", null, LocalDate.now(),
                        TransactionTypeEnum.INCOME,
                        null, List.of(), 1L));
    }

    @Test
    void noGlobalPriceAndNoDetails_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionUpdateCommand("teszt", null, LocalDate.now(),
                        TransactionTypeEnum.INCOME,
                        List.of(), List.of(), 1L));
    }

    @Test
    void globalPriceAndDetails_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionUpdateCommand("teszt", new BigDecimal("300"), LocalDate.now(),
                        TransactionTypeEnum.INCOME,
                        List.of(new TransactionDetailSaveCommand("teszt", new BigDecimal("400"),
                                List.of(), TransactionTypeEnum.INCOME)),
                        List.of(), 1L));
    }

    /**
     * A közös mezőket (név/dátum/típus) az alaposztály validálja, itt csak azt
     * ellenőrizzük, hogy ez a leszármazottnál is érvényesül
     */
    @Test
    void blankName_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionUpdateCommand("", null, LocalDate.now(),
                        TransactionTypeEnum.INCOME,
                        List.of(detail), List.of(), 1L));
    }

    @Test
    void validInput_assignsAllFields() {
        LocalDate date = LocalDate.of(2026, 5, 20);

        var command = new TransactionUpdateCommand("updated", null, date,
                TransactionTypeEnum.OUTCOME, List.of(detail), List.of(3L), 1L);

        assertEquals("updated", command.getTransactionName());
        assertEquals(null, command.getGlobalPrice());
        assertEquals(date, command.getTransactionDate());
        assertEquals(TransactionTypeEnum.OUTCOME, command.getTransactionType());
        assertEquals(List.of(detail), command.getDetailCommands());
        assertEquals(List.of(3L), command.getCategories());
    }
}
