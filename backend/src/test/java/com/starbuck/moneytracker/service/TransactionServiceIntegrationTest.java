package com.starbuck.moneytracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.TransactionTypeEnum;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@ActiveProfiles("test")
// csak így használható a beforeall, mert egyébként statikusan futna és nem
// elérhető az injektált dolgok
@TestInstance(TestInstance.Lifecycle.PER_CLASS)

class TransactionServiceIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepo;

    @Autowired
    private TransactionDetailRepository transactionDetailRepo;

    @Autowired
    private UserRepository userRepo;

    @PersistenceContext
    EntityManager em;

    private User user = null;

    @BeforeAll
    void beforeAll() {
        User user = new User("testuser", "password", "teszt@email.com");
        user.setUuid();
        this.user = userRepo.save(user);
    }

    @AfterAll
    void afterAll() {
        userRepo.delete(this.user);
    }

    /**
     * Létrehoz és megnézi, hogy bekerült-e minden jól a db-ben
     * Transactional, hogy töröljön mindent a teszt után, hogy ne maradjon a db-ben
     * semmi
     */
    @Transactional
    @Test
    void createTransaction_persistsBothEntities() {
        // GIVEN
        Transaction transaction = new Transaction();
        transaction.setName("Test");
        transaction.setTransactionType(TransactionTypeEnum.INCOME);
        transaction.setTransactionDate(LocalDate.of(2026, 6, 8));
        transaction.setUser(this.user);

        TransactionDetail transactionDetail = new TransactionDetail();
        transactionDetail.setPrice(new BigDecimal(100.00));

        List<TransactionDetail> transactionDetails = Arrays.asList(transactionDetail);

        // WHEN
        Transaction saved = transactionService.createTransaction(transaction, transactionDetails);

        // THEN
        assertNotNull(saved.getId());

        TransactionDetail detail = transactionDetailRepo.findAll().get(0);
        assertEquals(transactionService.DEFAULT_DETAIL_NAME, detail.getName());
        assertEquals(saved.getId(), detail.getTransaction().getId());

        assertEquals(1, transactionRepo.count());
        assertEquals(1, transactionDetailRepo.count());
        assertEquals(new BigDecimal(100.00), saved.getPriceSum());
    }

    @Test
    void createTransaction_throwsExceptionAndRollsBack() {
        Transaction transaction = new Transaction();
        transaction.setName("hibásteszt");
        transaction.setTransactionType(TransactionTypeEnum.INCOME);
        transaction.setTransactionDate(LocalDate.of(2026, 6, 8));
        transaction.setUser(this.user);

        TransactionDetail transactionDetail = new TransactionDetail();
        transactionDetail.setPrice(new BigDecimal(100));
        // emiatt nem fogja tudni elmenteni a detailst és rollback az egész
        transactionDetail.setName("hosszunev0".repeat(26));
        List<TransactionDetail> transactionDetails = Arrays.asList(transactionDetail);

        assertThrows(DataIntegrityViolationException.class, () -> {
            transactionService.createTransaction(transaction, transactionDetails);
        });

        // rollback miatt nincs egy sem a db-ben
        assertEquals(0, transactionRepo.count());
        assertEquals(0, transactionDetailRepo.count());
    }
}
