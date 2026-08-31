package com.starbuck.moneytracker.commands;

import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;

public abstract class AbstractWalletSaveCommand {
    
    protected String name;
    protected WalletTypeEnum type;

    public AbstractWalletSaveCommand(String name, WalletTypeEnum type) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public WalletTypeEnum getType() {
        return type;
    }
}
