package com.starbuck.moneytracker.unit.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import org.junit.jupiter.api.Test;

import com.starbuck.moneytracker.commands.UserCreateCommand;

class UserCreateCommandTest {

    @Test
    void blankUsername_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new UserCreateCommand("  ", "teszt@email.com", "jelszo123"));
    }

    @Test
    void blankPassword_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new UserCreateCommand("user", "teszt@email.com", "  "));
    }

    @Test
    void blankEmail_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new UserCreateCommand("user", "  ", "jelszo123"));
    }

    @Test
    void nullFields_throw() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new UserCreateCommand(null, "teszt@email.com", "jelszo123"));
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new UserCreateCommand("user", "teszt@email.com", null));
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new UserCreateCommand("user", null, "jelszo123"));
    }

    @Test
    void validInput_assignsAllFields() {
        var command = new UserCreateCommand("user", "teszt@email.com", "jelszo123");

        assertEquals("user", command.getUsername());
        assertEquals("teszt@email.com", command.getEmail());
        assertEquals("jelszo123", command.getPassword());
    }
}
