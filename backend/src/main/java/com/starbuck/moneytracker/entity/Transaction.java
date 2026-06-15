package com.starbuck.moneytracker.entity;

import java.time.LocalDateTime;
import java.util.Set;

import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToMany;

@Entity(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private String transaction_date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionTpye transaction_type;

    @CreatedDate
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "transaction")
    private Set<TransactionDetail> transactionDetails;

    public Transaction() {}
    
    public Transaction(Long id, String name, String transaction_date, TransactionTpye transaction_type,
            LocalDateTime createdAt, LocalDateTime updatedAt, Set<TransactionDetail> transactionDetails) {
        this.id = id;
        this.name = name;
        this.transaction_date = transaction_date;
        this.transaction_type = transaction_type;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.transactionDetails = transactionDetails;
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

    public String getTransaction_date() {
        return transaction_date;
    }

    public void setTransaction_date(String transaction_date) {
        this.transaction_date = transaction_date;
    }

    public TransactionTpye getTransaction_type() {
        return transaction_type;
    }

    public void setTransaction_type(TransactionTpye transaction_type) {
        this.transaction_type = transaction_type;
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

    public Set<TransactionDetail> getTransactionDetails() {
        return transactionDetails;
    }

    public void setTransactionDetails(Set<TransactionDetail> transactionDetails) {
        this.transactionDetails = transactionDetails;
    }

}
