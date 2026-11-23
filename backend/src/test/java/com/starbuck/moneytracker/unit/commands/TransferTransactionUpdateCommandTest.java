package com.starbuck.moneytracker.unit.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.starbuck.moneytracker.commands.TransferTransactionUpdateCommand;

class TransferTransactionUpdateCommandTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 16);
    private static final BigDecimal PRICE = new BigDecimal("100.00");

    @Test
    void validInput_setsFields() {
        var command = new TransferTransactionUpdateCommand("Kimenő", "Bejövő", DATE, PRICE);

        assertEquals("Kimenő", command.getSrcTransactionName());
        assertEquals("Bejövő", command.getDstTransactionName());
        assertEquals(DATE, command.getTransactionDate());
        assertEquals(PRICE, command.getPriceSum());
    }

    @Test
    void zeroPrice_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new TransferTransactionUpdateCommand("Kimenő", "Bejövő", DATE, BigDecimal.ZERO));
    }
}
