package com.starbuck.moneytracker.unit.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.starbuck.moneytracker.commands.TransactionCreateCommand;
import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;

/**
 * A TransactionCreateCommand nem ad hozzá extra validációt a TransactionSaveCommand-hoz
 * képest, csak leszármazik belőle - itt csak ezt ellenőrizzük.
 */
class TransactionCreateCommandTest {

    @Test
    void isATransactionSaveCommand() {
        var command = new TransactionCreateCommand("teszt", new BigDecimal("300"), LocalDate.now(), TransactionTypeEnum.INCOME,
                List.of(), List.of(), 1L);

        assertTrue(command instanceof TransactionSaveCommand);
    }

    @Test
    void allowsEmptyDetailList() {
        var command = new TransactionCreateCommand("teszt", new BigDecimal("10"), LocalDate.now(),
                TransactionTypeEnum.INCOME, List.of(), List.of(), 1L);

        assertEquals(0, command.getDetailCommands().size());
    }
}
