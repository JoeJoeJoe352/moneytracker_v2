package com.starbuck.moneytracker.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Limit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.starbuck.moneytracker.commands.TransactionDetailSaveCommand;
import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.commands.UserCreateCommand;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;
import com.starbuck.moneytracker.repository.TransactionDetailCategoryRepository;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.repository.UserRepository;
import com.starbuck.moneytracker.repository.WalletRepository;
import com.starbuck.moneytracker.service.TransactionService;
import com.starbuck.moneytracker.service.UserService;
import com.starbuck.moneytracker.testsupport.MySqlContainerTest;
import com.starbuck.moneytracker.util.CurrentUserUtil;

/**
 * Az integritás vizsgálat lapozó lekérdezéseinek tesztjei valódi adatbázison
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransactionRepositoryIntegrationTest extends MySqlContainerTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepo;

    @Autowired
    private TransactionDetailRepository transactionDetailRepo;

    @Autowired
    private TransactionDetailCategoryRepository detailCategoryRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private WalletRepository walletRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CurrentUserUtil currentUser;

    private User user;
    private Wallet wallet;

    @BeforeAll
    void beforeAll() {
        UserCreateCommand command = new UserCreateCommand("repotestuser", "repoteszt@email.com", "password");
        this.user = userService.createUser(command);
        this.wallet = walletRepo.findByUserId(this.user.getId()).get(0);
    }

    @BeforeEach
    void mockUserUtil() {
        Mockito.when(currentUser.getUser()).thenReturn(this.user);
    }

    @AfterEach
    void cleanUp() {
        detailCategoryRepo.deleteAllInBatch();
        transactionDetailRepo.deleteAllInBatch();
        // Natív sql, mert a soft delete miatt a repository a törölt tranzakciókat nem látja
        jdbcTemplate.update(
                "DELETE FROM transactions WHERE wallet_id IN (SELECT id FROM wallet WHERE user_id = ?)",
                this.user.getId());
    }

    @AfterAll
    void afterAll() {
        walletRepo.delete(this.wallet);
        userRepo.delete(this.user);
    }

    /**
     * Id szerint növekvő sorrendben, a megadott id utántól, legfeljebb limit
     * darabot ad vissza, a soft delete-elt tranzakciókat kihagyja
     */
    @Test
    void getActiveTransactionIdsAfter_returnsActiveIdsAfterLastIdInOrder() {
        long first = persistTransaction(new BigDecimal("1.00")).getId();
        long second = persistTransaction(new BigDecimal("2.00")).getId();
        Transaction deleted = persistTransaction(new BigDecimal("3.00"));
        long fourth = persistTransaction(new BigDecimal("4.00")).getId();
        transactionRepo.delete(deleted);

        List<Long> firstPage = transactionRepo.getActiveTransactionIdsAfter(first - 1, Limit.of(2));
        List<Long> secondPage = transactionRepo.getActiveTransactionIdsAfter(second, Limit.of(2));

        assertEquals(List.of(first, second), firstPage);
        assertEquals(List.of(fourth), secondPage);
    }

    /**
     * A részlet nélküli tranzakciót is visszaadja (LEFT JOIN), a több
     * részletest pedig csak egyszer (DISTINCT), az összes részletével
     */
    @Test
    void getTransactionsWithDetailsByIds_includesTransactionsWithoutDetailsOnce() {
        Transaction withTwoDetails = persistTransaction(new BigDecimal("1.00"), new BigDecimal("2.00"));
        Transaction withoutDetails = persistTransaction(new BigDecimal("3.00"));
        transactionDetailRepo.deleteAllInBatch(transactionDetailRepo.findAll().stream()
                .filter(detail -> detail.getTransaction().getId().equals(withoutDetails.getId()))
                .toList());

        List<Transaction> result = transactionRepo.getTransactionsWithDetailsByIds(
                List.of(withTwoDetails.getId(), withoutDetails.getId()));

        Map<Long, Integer> detailCountById = result.stream()
                .collect(Collectors.toMap(
                        Transaction::getId, t -> t.getTransactionDetails().size()));
        assertEquals(2, result.size());
        assertEquals(Map.of(withTwoDetails.getId(), 2, withoutDetails.getId(), 0), detailCountById);
    }

    /**
     * Segédfüggvény: tranzakció létrehozása a megadott árú detailekkel
     */
    private Transaction persistTransaction(BigDecimal... prices) {
        List<TransactionDetailSaveCommand> details = List.of(prices).stream()
                .map(price -> new TransactionDetailSaveCommand(
                        TransactionDetail.DEFAULT_DETAIL_NAME, price, List.of(), TransactionTypeEnum.INCOME))
                .toList();
        TransactionSaveCommand command = new TransactionSaveCommand(
                "Test",
                null,
                LocalDate.of(2026, 6, 8),
                TransactionTypeEnum.INCOME,
                details,
                List.of(),
                this.wallet.getId());

        return transactionService.createTransaction(command);
    }
}
