package com.starbuck.moneytracker.commands;

import java.math.BigDecimal;

public class BalanceSyncCommand {

    private long walletId;
    private BigDecimal balanceFromUser;

    public BalanceSyncCommand(long walletId, BigDecimal balanceFromUser) {
        // Itt lehet 0 is, ha még nincs egyáltalán pénz a tárcában
        if (balanceFromUser == null) {
            throw new IllegalArgumentException("BalanceFromUser cannot be null");
        }

        this.walletId = walletId;
        this.balanceFromUser = balanceFromUser;
    }

    public long getWalletId() {
        return walletId;
    }

    public BigDecimal getBalanceFromUser() {
        return balanceFromUser;
    }

}
