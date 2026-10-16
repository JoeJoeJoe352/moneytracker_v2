package com.starbuck.moneytracker.unit.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import org.junit.jupiter.api.Test;

import com.starbuck.moneytracker.commands.UserLoginCommand;

class UserLoginCommandTest {

    @Test
    void blankEmail_throws() {
        assertThrowsExactly(IllegalArgumentException.class, () -> new UserLoginCommand("  ", "jelszo123"));
    }

    @Test
    void blankPassword_throws() {
        assertThrowsExactly(IllegalArgumentException.class, () -> new UserLoginCommand("user@email.com", "  "));
    }

    @Test
    void nullFields_throw() {
        assertThrowsExactly(IllegalArgumentException.class, () -> new UserLoginCommand(null, "jelszo123"));
        assertThrowsExactly(IllegalArgumentException.class, () -> new UserLoginCommand("user@email.com", null));
    }

    @Test
    void validInput_assignsAllFields() {
        var command = new UserLoginCommand("user@email.com", "jelszo123");

        assertEquals("user@email.com", command.getEmail());
        assertEquals("jelszo123", command.getPassword());
    }
}
