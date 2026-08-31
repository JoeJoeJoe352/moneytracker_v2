package com.starbuck.moneytracker.commands;

import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;

public class UpdateWalletCommand extends AbstractWalletSaveCommand {

    public UpdateWalletCommand(String name, WalletTypeEnum type) {
        super(name, type);
    }
}
