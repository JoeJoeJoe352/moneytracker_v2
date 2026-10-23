package com.starbuck.moneytracker.unit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.starbuck.moneytracker.commands.TransactionDetailSaveCommand;
import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;
import com.starbuck.moneytracker.mapper.TransactionMapper;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.service.TransactionIntegrityChecker;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class TransactionIntegrityCheckerTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper mapper;

    @Mock
    private EntityManager entityManager;

    private TransactionIntegrityChecker checker;

    @BeforeEach
    void setUp() {
        checker = new TransactionIntegrityChecker(transactionRepository, mapper, entityManager);
    }

    /**
     * Lapokban tölti be a tranzakciókat: a következő lapot mindig az előző
     * lap utolsó id-ja után kéri, üres lapnál megáll, és minden lap után
     * üríti a persistence contextet
     */
    @Test
    void checkTransactionIntegrity_loadsTransactionsInBatches() {
        when(transactionRepository.getActiveTransactionIdsAfter(eq(0L), any())).thenReturn(List.of(1L, 2L));
        when(transactionRepository.getActiveTransactionIdsAfter(eq(2L), any())).thenReturn(List.of(3L));
        when(transactionRepository.getActiveTransactionIdsAfter(eq(3L), any())).thenReturn(List.of());
        when(transactionRepository.getTransactionsWithDetailsByIds(any())).thenReturn(List.of());

        checker.checkTransactionIntegrity();

        verify(transactionRepository).getTransactionsWithDetailsByIds(List.of(1L, 2L));
        verify(transactionRepository).getTransactionsWithDetailsByIds(List.of(3L));
        verify(transactionRepository, times(3)).getActiveTransactionIdsAfter(anyLong(), any());
        verify(entityManager, times(2)).clear();
    }

    /**
     * Ha egy tranzakció ellenőrzése kivételt dob, a többit attól még
     * ellenőrzi
     */
    @Test
    void checkTransactionIntegrity_continuesAfterExceptionInOneTransaction() {
        Transaction broken = transactionWithDetail(1L, new BigDecimal("10.00"));
        Transaction valid = transactionWithDetail(2L, new BigDecimal("10.00"));

        when(transactionRepository.getActiveTransactionIdsAfter(eq(0L), any())).thenReturn(List.of(1L, 2L));
        when(transactionRepository.getActiveTransactionIdsAfter(eq(2L), any())).thenReturn(List.of());
        when(transactionRepository.getTransactionsWithDetailsByIds(List.of(1L, 2L)))
                .thenReturn(List.of(broken, valid));
        when(mapper.entityToCommand(broken)).thenThrow(new IllegalStateException("invalid row"));
        when(mapper.entityToCommand(valid)).thenReturn(commandWithPrice(new BigDecimal("10.00")));

        checker.checkTransactionIntegrity();

        verify(mapper).entityToCommand(valid);
        verify(entityManager).clear();
    }

    /**
     * Részlet nélküli tranzakciónál nem számol (a mapper a price_sum-ot adná
     * vissza, így az összehasonlításnak nincs értelme)
     */
    @Test
    void checkTransactionIntegrity_skipsCalculationForTransactionWithoutDetails() {
        Transaction withoutDetails = new Transaction();
        withoutDetails.setId(1L);
        withoutDetails.setTransactionDetails(List.of());

        when(transactionRepository.getActiveTransactionIdsAfter(eq(0L), any())).thenReturn(List.of(1L));
        when(transactionRepository.getActiveTransactionIdsAfter(eq(1L), any())).thenReturn(List.of());
        when(transactionRepository.getTransactionsWithDetailsByIds(List.of(1L))).thenReturn(List.of(withoutDetails));

        checker.checkTransactionIntegrity();

        verify(mapper, never()).entityToCommand(any());
    }

    private Transaction transactionWithDetail(long id, BigDecimal priceSum) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setPriceSum(priceSum);
        transaction.setTransactionDetails(List.of(new TransactionDetail()));
        return transaction;
    }

    private TransactionSaveCommand commandWithPrice(BigDecimal price) {
        TransactionDetailSaveCommand detail = new TransactionDetailSaveCommand(
                TransactionDetail.DEFAULT_DETAIL_NAME, price, List.of(), TransactionTypeEnum.INCOME);
        return new TransactionSaveCommand(
                "Test", null, LocalDate.of(2026, 6, 8), TransactionTypeEnum.INCOME, List.of(detail), List.of(), 1L);
    }
}
