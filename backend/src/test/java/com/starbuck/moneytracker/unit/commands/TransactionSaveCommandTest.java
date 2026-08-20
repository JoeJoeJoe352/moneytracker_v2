package com.starbuck.moneytracker.unit.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.starbuck.moneytracker.commands.TransactionCreateCommand;
import com.starbuck.moneytracker.commands.TransactionDetailSaveCommand;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;

/**
 * A TransactionSaveCommand absztrakt osztályt a konkrét TransactionCreateCommand
 * leszármazottján keresztül teszteli, mert az alaposztálynak nincs saját
 * extra validációja.
 */
class TransactionSaveCommandTest {

    @Test
    void nullName_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionCreateCommand(null, null, LocalDate.now(), TransactionTypeEnum.INCOME,
                        List.of(), List.of()));
    }

    @Test
    void blankName_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionCreateCommand("   ", null, LocalDate.now(), TransactionTypeEnum.INCOME,
                        List.of(), List.of()));
    }

    @Test
    void nullDate_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionCreateCommand("teszt", null, null, TransactionTypeEnum.INCOME,
                        List.of(), List.of()));
    }

    @Test
    void nullType_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new TransactionCreateCommand("teszt", null, LocalDate.now(), null,
                        List.of(), List.of()));
    }

    @Test
    void validInput_assignsAllFields() {
        TransactionDetailSaveCommand detail = new TransactionDetailSaveCommand("tétel", new BigDecimal("100"),
                List.of(), TransactionTypeEnum.INCOME);
        LocalDate date = LocalDate.of(2026, 3, 1);

        var command = new TransactionCreateCommand("teszt", new BigDecimal("500"), date,
                TransactionTypeEnum.INCOME, List.of(detail), List.of(1L, 2L));

        assertEquals("teszt", command.getTransactionName());
        assertEquals(new BigDecimal("500"), command.getGlobalPrice());
        assertEquals(date, command.getTransactionDate());
        assertEquals(TransactionTypeEnum.INCOME, command.getTransactionType());
        assertEquals(List.of(detail), command.getDetailCommands());
        assertEquals(List.of(1L, 2L), command.getCategories());
    }

    @Test
    void nullGlobalPriceAndEmptyLists_areAccepted() {
        var command = new TransactionCreateCommand("teszt", null, LocalDate.now(), TransactionTypeEnum.INCOME,
                List.of(), List.of());

        assertNull(command.getGlobalPrice());
        assertTrue(command.getDetailCommands().isEmpty());
        assertTrue(command.getCategories().isEmpty());
    }
}
