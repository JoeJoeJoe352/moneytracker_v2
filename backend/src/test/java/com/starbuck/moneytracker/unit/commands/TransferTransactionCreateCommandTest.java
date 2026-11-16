package com.starbuck.moneytracker.unit.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.starbuck.moneytracker.commands.TransferTransactionCreateCommand;

class TransferTransactionCreateCommandTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 16);
    private static final BigDecimal PRICE = new BigDecimal("100.00");

    @Test
    void validInput_setsFields() {
        var command = new TransferTransactionCreateCommand("Kimenő", 1L, "Bejövő", 2L, DATE, PRICE);

        assertEquals("Kimenő", command.getSrcTransactionName());
        assertEquals(1L, command.getSrcWalletId());
        assertEquals("Bejövő", command.getDstTransactionName());
        assertEquals(2L, command.getDstWalletId());
        assertEquals(DATE, command.getTransactionDate());
        assertEquals(PRICE, command.getPriceSum());
    }

    @Test
    void blankSrcName_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new TransferTransactionCreateCommand("  ", 1L, "Bejövő", 2L, DATE, PRICE));
    }

    @Test
    void nullDstName_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new TransferTransactionCreateCommand("Kimenő", 1L, null, 2L, DATE, PRICE));
    }

    @Test
    void nullDate_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new TransferTransactionCreateCommand("Kimenő", 1L, "Bejövő", 2L, null, PRICE));
    }

    @Test
    void nullPrice_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new TransferTransactionCreateCommand("Kimenő", 1L, "Bejövő", 2L, DATE, null));
    }

    @Test
    void zeroPrice_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new TransferTransactionCreateCommand("Kimenő", 1L, "Bejövő", 2L, DATE, BigDecimal.ZERO));
    }

    @Test
    void negativePrice_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new TransferTransactionCreateCommand(
                        "Kimenő", 1L, "Bejövő", 2L, DATE, new BigDecimal("-100.00")));
    }

    @Test
    void nullSrcWalletId_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new TransferTransactionCreateCommand("Kimenő", null, "Bejövő", 2L, DATE, PRICE));
    }

    @Test
    void nullDstWalletId_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new TransferTransactionCreateCommand("Kimenő", 1L, "Bejövő", null, DATE, PRICE));
    }

    @Test
    void sameSrcAndDstWallet_throws() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new TransferTransactionCreateCommand("Kimenő", 1L, "Bejövő", 1L, DATE, PRICE));
    }
}
