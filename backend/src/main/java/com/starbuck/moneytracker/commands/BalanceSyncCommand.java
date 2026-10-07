package com.starbuck.moneytracker.commands;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BalanceSyncCommand {

    private long walletId;
    private LocalDate syncDate;
    private BigDecimal balanceFromUser;

    public BalanceSyncCommand(long walletId, LocalDate syncDate, BigDecimal balanceFromUser) {
        if (syncDate == null) {
            throw new IllegalArgumentException("SyncDate code cannot be null");
        }
        // TODO validálni, hogy user a saját időzónájához képest ne tudjon jövőbeni időt beállítani
        // Itt lehet 0 is, ha még nincs egyáltalán pénz a tárcában
        if (balanceFromUser == null) {
            throw new IllegalArgumentException("currentBalance cannot be null");
        }

        this.walletId = walletId;
        this.syncDate = syncDate;
        this.balanceFromUser = balanceFromUser;
    }

    public long getWalletId() {
        return walletId;
    }

    public LocalDate getSyncDate() {
        return syncDate;
    }

    public BigDecimal getBalanceFromUser() {
        return balanceFromUser;
    }

}
