package com.starbuck.moneytracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepo;

    @Mock
    private TransactionDetailRepository transactionDetailRepo;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void createTransaction_savesTransactionAndDetail() {
        // GIVEN - előkészületek
        Transaction transaction = new Transaction();
        TransactionDetail detail = new TransactionDetail();

        List<TransactionDetail> transactionDetails = Arrays.asList(detail);

        // dummy tranzakció, csak azok az adatokkal kitöltve, amik számítanak
        Transaction savedTransaction = new Transaction();
        savedTransaction.setId(1L);

        // ha ment a save meghívódik, akkor csak visszaadja a savedTransaction példányt
        Mockito.when(transactionRepo.save(transaction))
                .thenReturn(savedTransaction);

        // WHEN - függvény meghívása
        Transaction result = transactionService.createTransaction(transaction, transactionDetails);

        // THEN
        // azzal tér vissza, amit a repository visszaadott neki
        assertEquals(savedTransaction, result);
        // ugyanaz a tranzakció osztály, ami a details-ben is meg van adva
        assertEquals(savedTransaction, detail.getTransaction());
        assertEquals("sum", detail.getName());

        // save mindkét repo-ban meg volt hívva
        Mockito.verify(transactionRepo).save(transaction);
        Mockito.verify(transactionDetailRepo).save(detail);
    }
}