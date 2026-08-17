package com.starbuck.moneytracker.dto;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.starbuck.moneytracker.entity.Transaction;

public record HistoryQueryHelperDto(
        int limit,
        Sort sort,
        Specification<Transaction> spec
    ) {

}
