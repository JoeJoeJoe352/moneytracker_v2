package com.starbuck.moneytracker.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.testutils.AssertUtil;
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

        private AssertUtil assertUtil;

        TransactionServiceTest() {
                this.assertUtil = new AssertUtil();
        }

        @BeforeEach
        void createTransactionSaveMock() {
                // ha a save meghívódik, akkor visszaadja a paraméterben kapott tranzakciót +
                // szimuláljuk egy id beírását
                Mockito.when(transactionRepo.save(any(Transaction.class))).thenAnswer(invocation -> {
                        Transaction invocatedTransaction = invocation.getArgument(0);
                        if (invocatedTransaction.getId() == null) {
                                invocatedTransaction.setId(1L);
                        }
                        return invocatedTransaction;
                });
        }

        /**
         * Teszteli a tranzakciót és egy hozzá tartozó detail elmentését
         */
        @Test
        void createTransaction_savesTransactionAndDetail() {
                // GIVEN - előkészületek
                Transaction transaction = new Transaction("simpleTransaction", LocalDate.now(),
                                TransactionTypeEnum.INCOME,
                                null);
                TransactionDetail detail = new TransactionDetail();
                detail.setPrice(new BigDecimal(100));

                List<TransactionDetail> transactionDetailList = Arrays.asList(detail);

                ArgumentCaptor<TransactionDetail> captor = ArgumentCaptor.forClass(TransactionDetail.class);

                // WHEN - függvény meghívása
                Transaction result = transactionService.createTransaction(transaction, transactionDetailList);

                // THEN
                // Elmentte a transaction objectet és a detail példányt is
                Mockito.verify(transactionRepo).save(transaction);
                Mockito.verify(transactionDetailRepo).save(captor.capture());

                assertUtil.assertTransaction(result, "simpleTransaction", LocalDate.now(), new BigDecimal("100.00"),
                                TransactionTypeEnum.INCOME);
                // csak egy, név nélküli tranzakció van, ezért default nevet kap
                assertUtil.assertDetail(captor.getValue(), TransactionDetail.DEFAULT_DETAIL_NAME,
                                new BigDecimal("100.00"), null,
                                null,
                                result);
        }

        /**
         * Teszteli egy tranzakció és több detail elmentését
         */
        @Test
        void createTransaction_savesTransactionAndMultipleDetail() {
                // GIVEN
                Transaction transaction = new Transaction("multipleDetailedTransaction", LocalDate.now(),
                                TransactionTypeEnum.INCOME,
                                null);
                TransactionDetail detail = new TransactionDetail("detail1", new BigDecimal(100));
                TransactionDetail detail2 = new TransactionDetail("detail2", new BigDecimal(200));

                List<TransactionDetail> transactionDetails = Arrays.asList(detail, detail2);

                ArgumentCaptor<TransactionDetail> captor = ArgumentCaptor.forClass(TransactionDetail.class);

                // WHEN
                Transaction result = transactionService.createTransaction(transaction, transactionDetails);

                // THEN
                // tranzakció és a két detail is el lett mentve
                Mockito.verify(transactionRepo).save(transaction);
                assertUtil.assertTransaction(result, "multipleDetailedTransaction", LocalDate.now(),
                                new BigDecimal("300.00"),
                                TransactionTypeEnum.INCOME);

                Mockito.verify(transactionDetailRepo, times(2)).save(captor.capture());

                var details = captor.getAllValues();

                assertUtil.assertDetail(details.get(0), "detail1", new BigDecimal("100.00"), null, null, result);
                assertUtil.assertDetail(details.get(1), "detail2", new BigDecimal("200.00"), null, null, result);
        }

        /**
         * Teszteli, hogy hibát dob-e a kód, ha több tranzakciót akarunk elmenteni, de
         * nincs név megadva
         */
        @Test
        void createTransaction_savesTransactionAndMultipleDetailWithoutName() {
                Transaction transaction = new Transaction();
                transaction.setTransactionType(TransactionTypeEnum.INCOME);
                TransactionDetail detail = new TransactionDetail();
                detail.setPrice(new BigDecimal(100));
                TransactionDetail detail2 = new TransactionDetail();
                detail2.setPrice(new BigDecimal(200));
                detail2.setName("detail2");

                List<TransactionDetail> transactionDetails = Arrays.asList(detail, detail2);

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
                // Ezeket úgy kezeljük, mintha már a db-ben lennének
                User userInDB = new User(1l, "alma", "pass", "email");
                Transaction transactionInDB = new Transaction(1l, "teszt", LocalDate.now(), TransactionTypeEnum.INCOME,
                                new BigDecimal(100), 0);
                TransactionDetail detailInDB = new TransactionDetail(1l, "detail1", new BigDecimal(100), null, null,
                                transactionInDB);
                transactionInDB.setTransactionDetails(Set.of(detailInDB));

                // Ezek lesznek azok, amikkel módosítjuk az adatokat
                Transaction updatedTransaction = new Transaction(1l, "updated", LocalDate.of(2023, 1, 1),
                                TransactionTypeEnum.OUTCOME,
                                new BigDecimal(100), 0);
                TransactionDetail updatedDetail = new TransactionDetail(1l, "updatedDetail", new BigDecimal(-200), null,
                                null,
                                updatedTransaction);
                TransactionDetail updatedDetail2 = new TransactionDetail(2l, "updatedDetail2", new BigDecimal(-300),
                                null, null,
                                updatedTransaction);

                Mockito.when(currentUser.getUser()).thenReturn(userInDB);
                Mockito.when(transactionRepo.getTransactionById(anyLong(), anyLong()))
                                .thenReturn(Optional.of(transactionInDB));

                ArgumentCaptor<Transaction> captorTransaction = ArgumentCaptor.forClass(Transaction.class);
                ArgumentCaptor<TransactionDetail> captorDetail = ArgumentCaptor.forClass(TransactionDetail.class);

                // WHEN
                transactionService.updateTransaction(1l, updatedTransaction, List.of(updatedDetail, updatedDetail2));

                // THEN - az új beállított értékeket menti el
                Mockito.verify(transactionRepo).save(captorTransaction.capture());
                Transaction savedTransaction = captorTransaction.getValue();
                assertUtil.assertTransaction(savedTransaction, "updated", LocalDate.of(2023, 1, 1),
                                new BigDecimal("-500.00"),
                                TransactionTypeEnum.OUTCOME);

                // törli az eddigi detailokat és elmenti az újakat
                Mockito.verify(transactionDetailRepo).deleteAll(Set.of(detailInDB));
                Mockito.verify(transactionDetailRepo, times(2)).save(captorDetail.capture());

                var details = captorDetail.getAllValues();
                assertUtil.assertDetail(details.get(0), "updatedDetail", new BigDecimal("-200.00"), null, null,
                                savedTransaction);
                assertUtil.assertDetail(details.get(1), "updatedDetail2", new BigDecimal("-300.00"), null, null,
                                savedTransaction);
        }

        @Test
        void testCreateTransactionWithWeightAndUnitPrice() {
                // GIVEN
                Transaction transaction = new Transaction("multipleDetailedTransactionWithWeightAndUnitPrice",
                                LocalDate.now(),
                                TransactionTypeEnum.INCOME,
                                null);

                TransactionDetail detail1 = new TransactionDetail("WeightesDetail", new BigDecimal("0.5"),
                                new BigDecimal("300"));
                TransactionDetail detail2 = new TransactionDetail("Simadetail", new BigDecimal("200"));
                List<TransactionDetail> transactionDetails = Arrays.asList(detail1, detail2);

                ArgumentCaptor<TransactionDetail> captor = ArgumentCaptor.forClass(TransactionDetail.class);

                // WHEN
                var result = transactionService.createTransaction(transaction, transactionDetails);

                // THEN
                // Tranzakció elmentve és helyesek az adatai
                Mockito.verify(transactionRepo).save(transaction);
                assertUtil.assertTransaction(result, "multipleDetailedTransactionWithWeightAndUnitPrice",
                                LocalDate.now(),
                                new BigDecimal("350.00"),
                                TransactionTypeEnum.INCOME);

                // detailok elmentve és helyesek az adatai
                Mockito.verify(transactionDetailRepo, times(2)).save(captor.capture());
                var details = captor.getAllValues();

                assertUtil.assertDetail(details.get(0), "WeightesDetail", new BigDecimal("150.00"),
                                new BigDecimal("0.5"),
                                new BigDecimal("300"),
                                result);
                assertUtil.assertDetail(details.get(1), "Simadetail", new BigDecimal("200.00"), null, null, result);
        }

        @Test
        void testUpdateTransactionWithWeightAndUnitPrice() {
                // GIVEN
                // "Db-ben" lévő dolgok
                User userInDB = new User(1l, "alma", "pass", "email");

                Transaction transactionInDB = new Transaction("multipleDetailedTransactionWithWeightAndUnitPrice",
                                LocalDate.now(),
                                TransactionTypeEnum.INCOME,
                                null);

                TransactionDetail detail1inDb = new TransactionDetail("WeightesDetail1", new BigDecimal("0.5"),
                                new BigDecimal(300));
                TransactionDetail detail2inDb = new TransactionDetail("Simadetail1", new BigDecimal(200));
                transactionInDB.setTransactionDetails(Set.of(detail1inDb, detail2inDb));

                // updatelő dolgok
                Transaction updatedTransaction = new Transaction(1l, "updated", LocalDate.of(2023, 1, 1),
                                TransactionTypeEnum.OUTCOME,
                                new BigDecimal(100), 0);
                // weightesből sima priceos, priceosból weightes tranzakciót csinálunk
                TransactionDetail updatedDetail = new TransactionDetail("simadetail1", new BigDecimal(-200));
                TransactionDetail updatedDetail2 = new TransactionDetail("weightresDetail2", new BigDecimal("0.7"),
                                new BigDecimal(300));

                ArgumentCaptor<Transaction> captorTransaction = ArgumentCaptor.forClass(Transaction.class);
                ArgumentCaptor<TransactionDetail> captorDetails = ArgumentCaptor.forClass(TransactionDetail.class);

                Mockito.when(currentUser.getUser()).thenReturn(userInDB);
                Mockito.when(transactionRepo.getTransactionById(anyLong(), anyLong()))
                                .thenReturn(Optional.of(transactionInDB));

                // WHEN
                transactionService.updateTransaction(1l, updatedTransaction, List.of(updatedDetail, updatedDetail2));

                // THEN
                // Tranzakció elmentve és helyesek az adatai
                Mockito.verify(transactionRepo).save(captorTransaction.capture());
                var transaction = captorTransaction.getValue();
                this.assertUtil.assertTransaction(transaction, "updated", LocalDate.of(2023, 1, 1),
                                new BigDecimal("-410.00"), TransactionTypeEnum.OUTCOME);

                // detailok elmentve és helyesek az adatai
                Mockito.verify(transactionDetailRepo, times(2)).save(captorDetails.capture());
                var details = captorDetails.getAllValues();
                assertUtil.assertDetail(details.get(0), "simadetail1", new BigDecimal("-200.00"), null, null, transaction);
                assertUtil.assertDetail(details.get(1), "weightresDetail2", new BigDecimal("-210.00"),
                                new BigDecimal("0.7"),
                                new BigDecimal("300"),
                                transaction);
        }
}