package com.starbuck.moneytracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
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

    /**
     * Teszteli a tranzakciót és egy hozzá tartozó detail elmentését
     */
    @Test
    void createTransaction_savesTransactionAndDetail() {
        // GIVEN - előkészületek
        Transaction transaction = new Transaction();
        TransactionDetail detail = new TransactionDetail();
        detail.setPrice(new BigDecimal(100));

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
        // csak egy, név nélküli tranzakció van, ezért default nevet kapott
        assertEquals(transactionService.DEFAULT_DETAIL_NAME, detail.getName());
        assertEquals(new BigDecimal(100), transaction.getPriceSum());

        // Megpróbálta elmenteni a transaction nevű és a detail nevű példányt is
        Mockito.verify(transactionRepo).save(transaction);
        Mockito.verify(transactionDetailRepo).save(detail);
    }

    /**
     * Teszteli egy tranzakció és több detail elmentését
     */
    @Test
    void createTransaction_savesTransactionAndMultipleDetail() {
        Transaction transaction = new Transaction();
        TransactionDetail detail = new TransactionDetail();
        detail.setPrice(new BigDecimal(100));
        detail.setName("detail1");
        TransactionDetail detail2 = new TransactionDetail();
        detail2.setPrice(new BigDecimal(200));
        detail2.setName("detail2");

        List<TransactionDetail> transactionDetails = Arrays.asList(detail, detail2);

        Transaction savedTransaction = new Transaction();
        savedTransaction.setId(1L);

        // ha ment a save meghívódik, akkor csak visszaadja a savedTransaction példányt
        Mockito.when(transactionRepo.save(transaction))
                .thenReturn(savedTransaction);

        Transaction result = transactionService.createTransaction(transaction, transactionDetails);

        assertEquals(savedTransaction, result);
        assertEquals(savedTransaction, detail.getTransaction());
        assertEquals("detail1", detail.getName()); // nem írta át a nevet
        assertEquals("detail2", detail2.getName()); // nem írta át a nevet
        assertEquals(new BigDecimal(300), transaction.getPriceSum());

        // tranzakció és a két detail is el lett mentve
        Mockito.verify(transactionRepo).save(transaction);
        Mockito.verify(transactionDetailRepo).save(detail);
        Mockito.verify(transactionDetailRepo).save(detail2);
    }

    /**
     * Teszteli, hogy hibát dob-e a kód, ha több tranzakciót akarunk elmenteni, de
     * nincs név megadva
     */
    @Test
    void createTransaction_savesTransactionAndMultipleDetailWithoutName() {
        Transaction transaction = new Transaction();
        TransactionDetail detail = new TransactionDetail();
        detail.setPrice(new BigDecimal(100));
        TransactionDetail detail2 = new TransactionDetail();
        detail2.setPrice(new BigDecimal(200));
        detail2.setName("detail2");

        List<TransactionDetail> transactionDetails = Arrays.asList(detail, detail2);

        Transaction savedTransaction = new Transaction();
        savedTransaction.setId(1L);

        // ha ment a save meghívódik, akkor csak visszaadja a savedTransaction példányt
        Mockito.when(transactionRepo.save(transaction))
                .thenReturn(savedTransaction);

        // hiba, mert több tranzakciót akarunk elmenteni, de nincs név megadva legalább
        // az egyiknél
        assertThrows(IllegalArgumentException.class, () -> {
            transactionService.createTransaction(transaction, transactionDetails);
        });

    }
}