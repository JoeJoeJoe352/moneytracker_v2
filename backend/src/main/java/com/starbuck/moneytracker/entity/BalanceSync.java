package com.starbuck.moneytracker.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.starbuck.moneytracker.entity.enum_entites.GeneralStatusEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

// soft delete (logikai törlés)
@SQLDelete(sql = "UPDATE balance_sync SET status = 1 WHERE id = ?")
// autogenerált sql-ekben csak a nem töröltek jelennek meg
@SQLRestriction("status = 0")
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

    /**
     * Az egyenleg, a sync bejegyzés létrejöttekor
     * TODO kitalálni, hogy ez kell-e a későbbiekben, vagy törölhető-e
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal actualBalance;

    @OneToOne(optional = true)
    @JoinColumn(name = "sync_transaction_id", referencedColumnName = "id", nullable = true)
    private Transaction syncTransaction;

    @Column(nullable = false)
    @ColumnDefault("0")
    private GeneralStatusEnum status = GeneralStatusEnum.ACTIVE;

    public BalanceSync() {
    }

    public BalanceSync(LocalDate syncDate, Wallet wallet, BigDecimal actualBalance) {
        this.syncDate = syncDate;
        this.wallet = wallet;
        this.actualBalance = actualBalance;
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

    public GeneralStatusEnum getStatus() {
        return status;
    }

    public void setStatus(GeneralStatusEnum status) {
        this.status = status;
    }

}
