package com.starbuck.moneytracker.commands;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;

public abstract class TransactionSaveCommand {
    protected BigDecimal globalPrice;
    protected String transactionName;
    protected LocalDate transactionDate;
    protected TransactionTypeEnum transactionType;
    protected List<TransactionDetailSaveCommand> detailCommands;
    protected List<Long> categories;

    /**
     * @param name
     * @param globalPrice
     * @param date
     * @param type
     * @param detailCommands
     * @param categories
     */
    public TransactionSaveCommand(String name, BigDecimal globalPrice, LocalDate date, TransactionTypeEnum type,
            List<TransactionDetailSaveCommand> detailCommands, List<Long> categories) {

        if (detailCommands.size() == 0 && globalPrice == null) {
            throw new IllegalArgumentException("Global Price must be set when there is no detail");
        }
        if (globalPrice != null) {
            type.validateDetailPrice(globalPrice);
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Transaction name cannot be empty");
        }
        if (date == null) {
            throw new IllegalArgumentException("Transaction date cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Transaction type cannot be null");
        }

        this.globalPrice = globalPrice;
        this.transactionName = name;
        this.transactionDate = date;
        this.transactionType = type;
        this.detailCommands = detailCommands;
        this.categories = categories;
    }

    public String getTransactionName() {
        return transactionName;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public TransactionTypeEnum getTransactionType() {
        return transactionType;
    }

    public BigDecimal getGlobalPrice() {
        return globalPrice;
    }

    public List<TransactionDetailSaveCommand> getDetailCommands() {
        return detailCommands;
    }

    public List<Long> getCategories() {
        return categories;
    }

}
