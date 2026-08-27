package com.starbuck.moneytracker.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;
import com.starbuck.moneytracker.entity.enum_entites.GeneralStatusEnum;
import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @ColumnDefault("0")
    private GeneralStatusEnum status = GeneralStatusEnum.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private CurrencyEnum currencyCode = null;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletTypeEnum type;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "wallet")
    private List<Transaction> transactions;

    public Wallet() {
    }

    public Wallet(String name, User user, CurrencyEnum currencyCode, WalletTypeEnum type) {
        this.name = name;
        this.user = user;
        this.currencyCode = currencyCode;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public GeneralStatusEnum getStatus() {
        return status;
    }

    public void setStatus(GeneralStatusEnum status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public CurrencyEnum getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyEnum currencyCode) {
        this.currencyCode = currencyCode;
    }

    public WalletTypeEnum getType() {
        return type;
    }

    public void setType(WalletTypeEnum type) {
        this.type = type;
    }
}
