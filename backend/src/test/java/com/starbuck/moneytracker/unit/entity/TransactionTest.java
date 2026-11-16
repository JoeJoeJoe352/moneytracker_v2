package com.starbuck.moneytracker.unit.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.enums.GeneralStatusEnum;
import com.starbuck.moneytracker.entity.enums.TransactionSpecialTypeEnum;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;

class TransactionTest {

    /**
     * A specialType nélküli konstruktorok sima (special type nélküli) tranzakciót
     * hoznak létre
     */
    @Test
    void constructorsWithoutSpecialType_leaveSpecialTypeNull() {
        Transaction withoutId =
                new Transaction("name", LocalDate.now(), TransactionTypeEnum.OUTCOME, new BigDecimal("-10.00"), null);
        Transaction withId = new Transaction(
                1L,
                "name",
                LocalDate.now(),
                TransactionTypeEnum.OUTCOME,
                new BigDecimal("-10.00"),
                GeneralStatusEnum.ACTIVE,
                null);

        assertNull(withoutId.getSpecialType());
        assertNull(withId.getSpecialType());
    }

    /**
     * A specialType-os konstruktorok beállítják a special type-ot és a többi mezőt
     */
    @Test
    void constructorsWithSpecialType_setSpecialType() {
        Transaction withoutId = new Transaction(
                "name",
                LocalDate.of(2026, 11, 16),
                TransactionTypeEnum.OUTCOME,
                TransactionSpecialTypeEnum.TRANSFER,
                new BigDecimal("-10.00"),
                null);
        Transaction withId = new Transaction(
                1L,
                "name",
                LocalDate.of(2026, 11, 16),
                TransactionTypeEnum.OUTCOME,
                TransactionSpecialTypeEnum.SYNC,
                new BigDecimal("-10.00"),
                GeneralStatusEnum.ACTIVE,
                null);

        assertEquals(TransactionSpecialTypeEnum.TRANSFER, withoutId.getSpecialType());
        assertEquals("name", withoutId.getName());
        assertEquals(LocalDate.of(2026, 11, 16), withoutId.getTransactionDate());
        assertEquals(new BigDecimal("-10.00"), withoutId.getPriceSum());

        assertEquals(TransactionSpecialTypeEnum.SYNC, withId.getSpecialType());
        assertEquals(1L, withId.getId());
        assertEquals(GeneralStatusEnum.ACTIVE, withId.getStatus());
    }

    /**
     * Az isTransferTransaction csak TRANSFER special type esetén igaz
     */
    @Test
    void isTransferTransaction_trueOnlyForTransferSpecialType() {
        assertTrue(createWithSpecialType(TransactionSpecialTypeEnum.TRANSFER).isTransferTransaction());
        assertFalse(createWithSpecialType(TransactionSpecialTypeEnum.SYNC).isTransferTransaction());
        assertFalse(createWithSpecialType(TransactionSpecialTypeEnum.RECURRING).isTransferTransaction());
        assertFalse(createWithSpecialType(null).isTransferTransaction());
    }

    private Transaction createWithSpecialType(TransactionSpecialTypeEnum specialType) {
        return new Transaction(
                "name", LocalDate.now(), TransactionTypeEnum.OUTCOME, specialType, new BigDecimal("-10.00"), null);
    }
}
