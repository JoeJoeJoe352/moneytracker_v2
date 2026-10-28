package com.starbuck.moneytracker.util;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.starbuck.moneytracker.entity.Transaction;

public record HistoryQueryHelper(int limit, int pageNumber, Sort sort, Specification<Transaction> spec) {}
