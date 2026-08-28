package com.starbuck.moneytracker.unit.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import org.junit.jupiter.api.Test;

import com.starbuck.moneytracker.commands.CreateWalletCommand;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;
import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;

class CreateWalletCommandTest {

    private final User user = new User("teszt", "jelszo", "teszt@example.com");

    @Test
    void nullName_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new CreateWalletCommand(null, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT, user));
    }

    @Test
    void blankName_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new CreateWalletCommand("   ", CurrencyEnum.HUF, WalletTypeEnum.DEFAULT, user));
    }

    @Test
    void nullCurrency_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new CreateWalletCommand("Tárca", null, WalletTypeEnum.DEFAULT, user));
    }

    @Test
    void nullType_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new CreateWalletCommand("Tárca", CurrencyEnum.HUF, null, user));
    }

    @Test
    void nullUser_throws() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new CreateWalletCommand("Tárca", CurrencyEnum.HUF, WalletTypeEnum.DEFAULT, null));
    }

    @Test
    void validInput_assignsAllFields() {
        var command = new CreateWalletCommand("Tárca", CurrencyEnum.HUF, WalletTypeEnum.DEFAULT, user);

        assertEquals("Tárca", command.getName());
        assertEquals(CurrencyEnum.HUF, command.getCurrency());
        assertEquals(WalletTypeEnum.DEFAULT, command.getType());
        assertEquals(user, command.getUser());
    }
}
