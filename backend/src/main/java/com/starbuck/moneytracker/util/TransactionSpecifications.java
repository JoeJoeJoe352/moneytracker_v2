package com.starbuck.moneytracker.util;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import com.starbuck.moneytracker.entity.Transaction;

public class TransactionSpecifications {

    public static Specification<Transaction> hasName(String name) {
        return (root, query, cb) ->
            name == null ? 
            null : 
            cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Transaction> hasDate(LocalDate date) {
        return (root, query, cb) ->
            date == null ? null : cb.equal(root.get("transactionDate"), date);
    }

    public static Specification<Transaction> hasUserId(Long userId) {
        return (root, query, cb) ->
                cb.equal(root.get("user").get("id"), userId);
    }
}
