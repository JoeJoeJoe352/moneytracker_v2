package com.starbuck.moneytracker.testutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.TransactionTypeEnum;

@Component
public class AssertUtil {
    public void assertDetail(
            TransactionDetail actual,
            String expectedName,
            BigDecimal expectedPrice,
            BigDecimal expectedWeight,
            BigDecimal expectedUnitPrice,
            Transaction expectedTransaction) {
        assertEquals(expectedName, actual.getName());
        assertEquals(expectedPrice, actual.getPrice());
        assertEquals(expectedWeight, actual.getWeight());
        assertEquals(expectedUnitPrice, actual.getUnitPrice());
        assertEquals(expectedTransaction, actual.getTransaction());
    }

    public void assertTransaction(
            Transaction actual,
            String expectedName,
            LocalDate expectedDate,
            BigDecimal expectedPriceSum,
            TransactionTypeEnum expectedType) {
        assertEquals(expectedName, actual.getName());
        assertEquals(expectedPriceSum, actual.getPriceSum());
        assertEquals(expectedDate, actual.getTransactionDate());
        assertEquals(expectedType, actual.getTransactionType());
    }
}
