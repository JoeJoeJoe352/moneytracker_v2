package com.starbuck.moneytracker.commands;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import com.starbuck.moneytracker.entity.enums.RecurrenceIntervalEnum;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;

public class RecurringTransactionCreateCommand {
    private String name;
    private BigDecimal priceSum;
    private LocalDate startDate;
    private RecurrenceIntervalEnum recurrenceInterval;
    private TransactionTypeEnum transactionType;
    private List<Long> categoryIds;
    private Long walletId;

    /**
     * @param name                  Ismétlődő tranzakció neve
     * @param priceSum              Az ismétlődő tranzakció összege
     * @param startDate             Az első tranzakció dátuma
     * @param recurrenceInterval    Ismétlődés gyakorisága
     * @param transactionType       Tranzakció típusa
     * @param categoryIds           Kategóriák (lehet üres)
     * @param walletId              Tranzakcióhoz tartozó wallet
     */
    public RecurringTransactionCreateCommand(
            String name,
            BigDecimal priceSum,
            LocalDate startDate,
            RecurrenceIntervalEnum recurrenceInterval,
            TransactionTypeEnum transactionType,
            List<Long> categoryIds,
            Long walletId) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Recurring transaction name cannot be empty");
        }
        if (priceSum == null) {
            throw new IllegalArgumentException("Price sum cannot be null");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (recurrenceInterval == null) {
            throw new IllegalArgumentException("Recurrence interval cannot be null");
        }
        if (transactionType == null) {
            throw new IllegalArgumentException("Transaction type cannot be null");
        }
        transactionType.validateDetailPrice(priceSum);
        if (categoryIds == null) {
            throw new IllegalArgumentException("CategoryIds cannot be null (use empty List instead)");
        }
        if (categoryIds.size() != new HashSet<>(categoryIds).size()) {
            throw new IllegalArgumentException("Duplicated categoryId found!");
        }
        if (walletId == null) {
            throw new IllegalArgumentException("WalletId cannot be null");
        }

        this.name = name;
        this.priceSum = priceSum;
        this.startDate = startDate;
        this.recurrenceInterval = recurrenceInterval;
        this.transactionType = transactionType;
        this.categoryIds = categoryIds;
        this.walletId = walletId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPriceSum() {
        return priceSum;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public RecurrenceIntervalEnum getRecurrenceInterval() {
        return recurrenceInterval;
    }

    public TransactionTypeEnum getTransactionType() {
        return transactionType;
    }

    public List<Long> getCategoryIds() {
        return categoryIds;
    }

    public Long getWalletId() {
        return walletId;
    }
}
