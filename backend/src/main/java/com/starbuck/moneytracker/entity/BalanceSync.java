package com.starbuck.moneytracker.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "balance_sync")
public class BalanceSync {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate syncDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal actualBalance;

    @OneToOne(optional = true)
    @JoinColumn(name = "sync_transaction_id", referencedColumnName = "id", nullable = true)
    private Transaction syncTransaction;

    public BalanceSync() {
    }

    public BalanceSync(LocalDate syncDate, Wallet wallet, BigDecimal actualBalance,
            Transaction syncTransaction) {
        this.syncDate = syncDate;
        this.wallet = wallet;
        this.actualBalance = actualBalance;
        this.syncTransaction = syncTransaction;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getSyncDate() {
        return syncDate;
    }

    public void setSyncDate(LocalDate syncDate) {
        this.syncDate = syncDate;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public BigDecimal getActualBalance() {
        return actualBalance;
    }

    public void setActualBalance(BigDecimal actualBalance) {
        this.actualBalance = actualBalance;
    }

    public Transaction getSyncTransaction() {
        return syncTransaction;
    }

    public void setSyncTransaction(Transaction syncTransaction) {
        this.syncTransaction = syncTransaction;
    }

}
