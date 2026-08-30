package com.starbuck.moneytracker.commands;

import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;
import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;

public class CreateWalletCommand {

    String name;
    CurrencyEnum currency;
    WalletTypeEnum type;
    User user;

    public CreateWalletCommand(String name, CurrencyEnum currency, WalletTypeEnum type, User user) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (currency == null) {
            throw new IllegalArgumentException("currency code cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (user == null) {
            throw new IllegalArgumentException("user cannot be null");
        }

        this.name = name;
        this.currency = currency;
        this.type = type;
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public CurrencyEnum getCurrency() {
        return currency;
    }

    public WalletTypeEnum getType() {
        return type;
    }

    public User getUser() {
        return user;
    }

}
