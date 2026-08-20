package com.starbuck.moneytracker.unit.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import org.junit.jupiter.api.Test;

import com.starbuck.moneytracker.commands.CategoryCreateCommand;

class CategoryCreateCommandTest {

    @Test
    void nullName_throws() {
        assertThrowsExactly(IllegalArgumentException.class, () -> new CategoryCreateCommand(null));
    }

    @Test
    void blankName_throws() {
        assertThrowsExactly(IllegalArgumentException.class, () -> new CategoryCreateCommand("   "));
    }

    @Test
    void validName_isAssigned() {
        var command = new CategoryCreateCommand("élelmiszer");

        assertEquals("élelmiszer", command.getName());
    }
}
