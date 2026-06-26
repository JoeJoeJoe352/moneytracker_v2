package com.starbuck.moneytracker.util;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import com.starbuck.moneytracker.entity.Transaction;

public class TransactionSpecifications {

    /**
     * Opcionális query paraméter, ami ha van, akkor a név-re szűr
     * 
     * @param name
     * @return
     */
    public static Specification<Transaction> hasName(String name) {
        return (root, query, cb) ->
            name == null ? 
            null : 
            cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    /**
     * Opcionális query paraméter, ami ha van, akkor a dátumra szűr
     * 
     * @param date
     * @return
     */
    public static Specification<Transaction> hasDate(LocalDate date) {
        return (root, query, cb) ->
            date == null ? null : cb.equal(root.get("transactionDate"), date);
    }

    /**
     * Query paraméter, ami a user azonosítóra szűr (nem opcionális)
     * 
     * @param userId
     * @return
     */
    public static Specification<Transaction> hasUserId(Long userId) {
        return (root, query, cb) ->
                cb.equal(root.get("user").get("id"), userId);
    }
}
