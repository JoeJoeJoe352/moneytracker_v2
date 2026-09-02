package com.starbuck.moneytracker.commands;

import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;
import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;

public class CreateWalletCommand extends AbstractWalletSaveCommand {

    CurrencyEnum currency;
    User user;

    public CreateWalletCommand(String name, CurrencyEnum currency, WalletTypeEnum type, User user) {
        super(name, type);

        if (currency == null) {
            throw new IllegalArgumentException("currency code cannot be null");
        }

        if (user == null) {
            throw new IllegalArgumentException("user cannot be null");
        }

        this.currency = currency;
        this.user = user;
    }

    public CurrencyEnum getCurrency() {
        return currency;
    }

    public User getUser() {
        return user;
    }

}
