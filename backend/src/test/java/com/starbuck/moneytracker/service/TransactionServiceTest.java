package com.starbuck.moneytracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.TransactionTypeEnum;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.util.CurrentUserUtil;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepo;

    @Mock
    private TransactionDetailRepository transactionDetailRepo;

    @Mock
    private CurrentUserUtil currentUser;

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

    /**
     * Tranzakció frissítése, ha több tranzakciótétel van.
     */
    @Test
    void testUpdateTransaction() {
        // GIVEN
        User userInDB = new User(1l, "alma", "pass", "email");
        Transaction transactionInDB = new Transaction(1l, "teszt", LocalDate.now(), TransactionTypeEnum.INCOME,
                new BigDecimal(100), 0);
        TransactionDetail detailInDB = new TransactionDetail(1l, "detail1", new BigDecimal(100), null, null,
                transactionInDB);
        transactionInDB.setTransactionDetails(Set.of(detailInDB));

        Mockito.when(currentUser.getUser()).thenReturn(userInDB);
        Mockito.when(transactionRepo.getTransactionById(anyLong(), anyLong()))
                .thenReturn(Optional.of(transactionInDB));
        // elkapja a mentéskori tranzakciót
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // WHEN
        Transaction updatedTransaction = new Transaction(1l, "updated", LocalDate.of(2023, 1, 1),
                TransactionTypeEnum.OUTCOME,
                new BigDecimal(100), 0);
        TransactionDetail updatedDetail = new TransactionDetail(1l, "updatedDetail", new BigDecimal(200), null, null,
                updatedTransaction);
        TransactionDetail updatedDetail2 = new TransactionDetail(2l, "updatedDetail2", new BigDecimal(300), null, null,
                updatedTransaction);

        transactionService.updateTransaction(1l, updatedTransaction, List.of(updatedDetail, updatedDetail2));

        // THEN - az új beállított értékeket menti el
        Mockito.verify(transactionRepo).save(captor.capture());
        Transaction savedTransaction = captor.getValue();
        assertEquals("updated", savedTransaction.getName());
        assertEquals(LocalDate.of(2023, 1, 1), savedTransaction.getTransactionDate());
        assertEquals(TransactionTypeEnum.OUTCOME, savedTransaction.getTransactionType());
        assertEquals(new BigDecimal(500), savedTransaction.getPriceSum());
        // törli az eddigi detailokat
        Mockito.verify(transactionDetailRepo).deleteAll(Set.of(detailInDB));
        // elmenti az új detailokat
        Mockito.verify(transactionDetailRepo).save(updatedDetail);
        Mockito.verify(transactionDetailRepo).save(updatedDetail2);
    }

}