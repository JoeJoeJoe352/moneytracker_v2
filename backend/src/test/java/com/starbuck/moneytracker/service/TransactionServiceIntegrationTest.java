package com.starbuck.moneytracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.TransactionType;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
// csak így használható a beforeall, mert egyébként statikusan futna és nem elérhető az injektált dolgok
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

    private User user = null;

    @BeforeAll
    void beforeAll() {
        User user = new User("testuser", "password", "teszt@email.com");
        user.setUuid();
        this.user = userRepo.save(user);
    }

    @Test
    void createTransaction_persistsBothEntities() {
        // GIVEN
        Transaction transaction = new Transaction();
        transaction.setName("Test");
        transaction.setTransactionType(TransactionType.INCOME);
        transaction.setTransactionDate(LocalDate.of(2026, 6, 8));
        transaction.setUser(this.user);

        TransactionDetail transactionDetails = new TransactionDetail();
        transactionDetails.setPrice(new BigDecimal(100.00));

        // WHEN
        Transaction saved = transactionService.createTransaction(transaction, transactionDetails);

        // THEN
        assertNotNull(saved.getId());

        TransactionDetail detail = transactionDetailRepo.findAll().get(0);
        assertEquals("sum", detail.getName());
        assertEquals(saved.getId(), detail.getTransaction().getId());

        assertEquals(1, transactionRepo.count());
        assertEquals(1, transactionDetailRepo.count());

        this.transactionDetailRepo.delete(transactionDetails);
        this.transactionRepo.delete(transaction);

        userRepo.delete(user);
    }

    @Test
    void createTransaction_throwsExceptionAndRollsBack() {
        Transaction transaction = new Transaction();
        transaction.setName("hibásteszt");
        transaction.setTransactionType(TransactionType.INCOME);
        transaction.setTransactionDate(LocalDate.of(2026, 6, 8));
        transaction.setUser(this.user);

        TransactionDetail transactionDetails = new TransactionDetail();

        // emiatt nem fogja tudni elmenteni a detailst és rollback az egész
        transactionDetails.setName("hosszunev0".repeat(26));

        // hibát generálunk: pl. null price
        transactionDetails.setPrice(null);

        assertThrows(IllegalArgumentException.class, () -> {
            transactionService.createTransaction(transaction, transactionDetails);
        });

        //rollback miatt nincs egy sem a db-ben
        assertEquals(0, transactionRepo.count());
        assertEquals(0, transactionDetailRepo.count());

        userRepo.delete(user);
    }
}
