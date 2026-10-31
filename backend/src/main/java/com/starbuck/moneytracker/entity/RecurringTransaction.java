package com.starbuck.moneytracker.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import com.starbuck.moneytracker.entity.enums.RecurrenceIntervalEnum;
import com.starbuck.moneytracker.entity.enums.RecurringTransactionStatusEnum;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.Table;

// soft delete (logikai törlés)
@SQLDelete(sql = "UPDATE recurring_transaction SET status = 'DELETED' WHERE id = ?")
// autogenerált sql-ekben csak a nem töröltek jelennek meg (a STOPPED, azaz
// szüneteltetett is)
@SQLRestriction("status <> 'DELETED'")
@Entity
@Table(name = "recurring_transaction")
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceSum;

    @Column(nullable = false)
    private LocalDate startDate;

    /** A következő tranzakció létrehozásának dátuma */
    @Column(nullable = false)
    private LocalDate nextOccurrenceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrenceIntervalEnum recurrenceInterval;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionTypeEnum transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ColumnDefault("'ACTIVE'")
    private RecurringTransactionStatusEnum status = RecurringTransactionStatusEnum.ACTIVE;

    @ManyToOne(optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @OneToMany(mappedBy = "recurringTransaction", cascade = CascadeType.REMOVE)
    private List<RecurringTransactionCategory> categoryLinks;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public RecurringTransaction() {}

    public RecurringTransaction(
            String name,
            BigDecimal priceSum,
            LocalDate startDate,
            RecurrenceIntervalEnum recurrenceInterval,
            TransactionTypeEnum transactionType,
            Wallet wallet) {
        this.name = name;
        this.priceSum = priceSum;
        this.startDate = startDate;
        this.nextOccurrenceDate = startDate;
        this.recurrenceInterval = recurrenceInterval;
        this.transactionType = transactionType;
        this.wallet = wallet;
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

    public BigDecimal getPriceSum() {
        return priceSum;
    }

    public void setPriceSum(BigDecimal priceSum) {
        this.priceSum = priceSum;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getNextOccurrenceDate() {
        return nextOccurrenceDate;
    }

    public void setNextOccurrenceDate(LocalDate nextOccurrenceDate) {
        this.nextOccurrenceDate = nextOccurrenceDate;
    }

    public RecurrenceIntervalEnum getRecurrenceInterval() {
        return recurrenceInterval;
    }

    public void setRecurrenceInterval(RecurrenceIntervalEnum recurrenceInterval) {
        this.recurrenceInterval = recurrenceInterval;
    }

    public TransactionTypeEnum getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionTypeEnum transactionType) {
        this.transactionType = transactionType;
    }

    public RecurringTransactionStatusEnum getStatus() {
        return status;
    }

    public void setStatus(RecurringTransactionStatusEnum status) {
        this.status = status;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public List<RecurringTransactionCategory> getCategoryLinks() {
        return categoryLinks;
    }

    public void setCategoryLinks(List<RecurringTransactionCategory> categoryLinks) {
        this.categoryLinks = categoryLinks;
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
}
