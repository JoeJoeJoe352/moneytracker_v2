package com.starbuck.moneytracker.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.starbuck.moneytracker.commands.RecurringTransactionSaveCommand;
import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.RecurringTransaction;
import com.starbuck.moneytracker.entity.RecurringTransactionCategory;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enums.CurrencyEnum;
import com.starbuck.moneytracker.entity.enums.RecurrenceIntervalEnum;
import com.starbuck.moneytracker.entity.enums.RecurringTransactionStatusEnum;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;
import com.starbuck.moneytracker.entity.enums.WalletTypeEnum;
import com.starbuck.moneytracker.exception.EntityNotFoundException;
import com.starbuck.moneytracker.repository.CategoryRepository;
import com.starbuck.moneytracker.repository.RecurringTransactionCategoryRepository;
import com.starbuck.moneytracker.repository.RecurringTransactionRepository;
import com.starbuck.moneytracker.service.RecurringTransactionService;
import com.starbuck.moneytracker.service.WalletService;
import com.starbuck.moneytracker.util.CurrentUserUtil;

@ExtendWith(MockitoExtension.class)
public class RecurringTransactionServiceTest {

    private static final LocalDate START_DATE = LocalDate.of(2026, 11, 1);

    @Mock
    private RecurringTransactionRepository recurringTransactionRepo;

    @Mock
    private RecurringTransactionCategoryRepository recurringTransactionCategoryRepo;

    @Mock
    private CategoryRepository categoryRepo;

    @Mock
    private WalletService walletService;

    @Mock
    private CurrentUserUtil currentUser;

    private RecurringTransactionService service;

    private final User user = new User(1L, "name", "password", "email");

    private final Wallet wallet = new Wallet(1L, "wallet", user, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT);

    private final Wallet otherWallet = new Wallet(2L, "other", user, CurrencyEnum.EUR, WalletTypeEnum.SAVINGS);

    @BeforeEach
    void setUp() {
        service = new RecurringTransactionService(
                recurringTransactionRepo, recurringTransactionCategoryRepo, categoryRepo, walletService, currentUser);
        // nem minden ág kérdezi le a usert (pl. kategória nélküli létrehozás)
        lenient().when(currentUser.getUser()).thenReturn(user);
    }

    @Test
    void testCreateRecurringTransaction() {
        when(walletService.getWalletById(1L)).thenReturn(wallet);
        when(recurringTransactionRepo.save(any(RecurringTransaction.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.createRecurringTransaction(command(START_DATE, List.of(), 1L));

        var captor = ArgumentCaptor.forClass(RecurringTransaction.class);
        verify(recurringTransactionRepo).save(captor.capture());
        var saved = captor.getValue();
        assertSame(saved, result);
        assertEquals("Netflix", saved.getName());
        assertEquals(new BigDecimal("-4500"), saved.getPriceSum());
        assertEquals(START_DATE, saved.getStartDate());
        assertEquals(START_DATE, saved.getNextOccurrenceDate());
        assertEquals(RecurrenceIntervalEnum.MONTHLY, saved.getRecurrenceInterval());
        assertEquals(TransactionTypeEnum.OUTCOME, saved.getTransactionType());
        assertEquals(RecurringTransactionStatusEnum.ACTIVE, saved.getStatus());
        assertSame(wallet, saved.getWallet());
    }

    @Test
    void testCreateRecurringTransactionWithoutCategories() {
        when(walletService.getWalletById(1L)).thenReturn(wallet);
        when(recurringTransactionRepo.save(any(RecurringTransaction.class))).thenAnswer(i -> i.getArgument(0));

        service.createRecurringTransaction(command(START_DATE, List.of(), 1L));

        verify(categoryRepo, never()).findAllById(anyList(), anyLong());
        verify(recurringTransactionCategoryRepo, never()).save(any());
    }

    @Test
    void testCreateRecurringTransactionWithCategories() {
        when(walletService.getWalletById(1L)).thenReturn(wallet);
        when(recurringTransactionRepo.save(any(RecurringTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(categoryRepo.findAllById(List.of(10L, 11L), 1L)).thenReturn(List.of(category(10L), category(11L)));

        var result = service.createRecurringTransaction(command(START_DATE, List.of(10L, 11L), 1L));

        var captor = ArgumentCaptor.forClass(RecurringTransactionCategory.class);
        verify(recurringTransactionCategoryRepo, times(2)).save(captor.capture());
        var links = captor.getAllValues();
        assertEquals(10L, links.get(0).getCategory().getId());
        assertEquals(11L, links.get(1).getCategory().getId());
        assertSame(result, links.get(0).getRecurringTransaction());
        assertSame(result, links.get(1).getRecurringTransaction());
    }

    @Test
    void testCreateRecurringTransactionWithForeignCategory() {
        when(walletService.getWalletById(1L)).thenReturn(wallet);
        when(recurringTransactionRepo.save(any(RecurringTransaction.class))).thenAnswer(i -> i.getArgument(0));
        // a 11-es kategória nem a useré, ezért nem jön vissza
        when(categoryRepo.findAllById(List.of(10L, 11L), 1L)).thenReturn(List.of(category(10L)));

        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> service.createRecurringTransaction(command(START_DATE, List.of(10L, 11L), 1L)));
        verify(recurringTransactionCategoryRepo, never()).save(any());
    }

    @Test
    void testCreateRecurringTransactionNotExistingWallet() {
        when(walletService.getWalletById(1L)).thenThrow(new EntityNotFoundException("Wallet not found"));

        assertThrowsExactly(
                EntityNotFoundException.class,
                () -> service.createRecurringTransaction(command(START_DATE, List.of(), 1L)));
        verify(recurringTransactionRepo, never()).save(any());
    }

    // UPDATE

    @Test
    void testUpdateRecurringTransaction() {
        var existing = existing();
        when(walletService.getWalletById(2L)).thenReturn(otherWallet);
        when(recurringTransactionRepo.findByIdForUser(5L, 1L)).thenReturn(Optional.of(existing));

        var command = new RecurringTransactionSaveCommand(
                "Spotify",
                new BigDecimal("2000"),
                START_DATE,
                RecurrenceIntervalEnum.WEEKLY,
                TransactionTypeEnum.INCOME,
                List.of(),
                2L,
                false);
        service.updateRecurringTransaction(5L, command);

        verify(recurringTransactionRepo).save(existing);
        assertEquals("Spotify", existing.getName());
        assertEquals(new BigDecimal("2000"), existing.getPriceSum());
        assertEquals(RecurrenceIntervalEnum.WEEKLY, existing.getRecurrenceInterval());
        assertEquals(TransactionTypeEnum.INCOME, existing.getTransactionType());
        assertSame(otherWallet, existing.getWallet());
    }

    /**
     * Ha a küldött dátum megegyezik a következő esedékességgel, a kezdő dátum
     * marad (pl. a hónap végi levonások miatt kell az eredeti nap)
     */
    @Test
    void testUpdateRecurringTransactionKeepsStartDate() {
        var existing = existing();
        existing.setNextOccurrenceDate(START_DATE.plusMonths(1));
        when(walletService.getWalletById(1L)).thenReturn(wallet);
        when(recurringTransactionRepo.findByIdForUser(5L, 1L)).thenReturn(Optional.of(existing));

        service.updateRecurringTransaction(5L, command(START_DATE.plusMonths(1), List.of(), 1L));

        assertEquals(START_DATE, existing.getStartDate());
        assertEquals(START_DATE.plusMonths(1), existing.getNextOccurrenceDate());
    }

    /**
     * Ha a következő előfordulás dátuma megváltozik, akkor új kezdő dátum lesz
     */
    @Test
    void testUpdateRecurringTransactionResetsStartDate() {
        var existing = existing();
        existing.setNextOccurrenceDate(START_DATE.plusMonths(1));
        when(walletService.getWalletById(1L)).thenReturn(wallet);
        when(recurringTransactionRepo.findByIdForUser(5L, 1L)).thenReturn(Optional.of(existing));

        var newNextOccurrenceDate = START_DATE.plusDays(10);
        service.updateRecurringTransaction(5L, command(newNextOccurrenceDate, List.of(), 1L));

        assertEquals(newNextOccurrenceDate, existing.getStartDate());
        assertEquals(newNextOccurrenceDate, existing.getNextOccurrenceDate());
    }

    /**
     * A régi kategória kapcsolatokat törli és flush-ol, mielőtt az újakat
     * elmentené (különben a unique key-be ütközne)
     */
    @Test
    void testUpdateRecurringTransactionReplacesCategoryLinks() {
        var existing = existing();
        var oldLinks = List.of(new RecurringTransactionCategory(existing, category(10L)));
        existing.setCategoryLinks(oldLinks);
        when(walletService.getWalletById(1L)).thenReturn(wallet);
        when(recurringTransactionRepo.findByIdForUser(5L, 1L)).thenReturn(Optional.of(existing));
        when(categoryRepo.findAllById(List.of(10L, 12L), 1L)).thenReturn(List.of(category(10L), category(12L)));

        service.updateRecurringTransaction(5L, command(START_DATE, List.of(10L, 12L), 1L));

        InOrder order = inOrder(recurringTransactionCategoryRepo);
        order.verify(recurringTransactionCategoryRepo).deleteAll(oldLinks);
        order.verify(recurringTransactionCategoryRepo).flush();
        order.verify(recurringTransactionCategoryRepo, times(2)).save(any(RecurringTransactionCategory.class));
    }

    @Test
    void testUpdateRecurringTransactionRestartsStopped() {
        var existing = existing();
        existing.setStatus(RecurringTransactionStatusEnum.STOPPED);
        when(walletService.getWalletById(1L)).thenReturn(wallet);
        when(recurringTransactionRepo.findByIdForUser(5L, 1L)).thenReturn(Optional.of(existing));

        service.updateRecurringTransaction(5L, command(START_DATE, List.of(), 1L, true));

        assertEquals(RecurringTransactionStatusEnum.ACTIVE, existing.getStatus());
        verify(recurringTransactionRepo).save(existing);
    }

    /**
     * Csak megállított ismétlődő tranzakció indítható újra
     */
    @Test
    void testUpdateRecurringTransactionRestartActive() {
        var existing = existing();
        when(walletService.getWalletById(1L)).thenReturn(wallet);
        when(recurringTransactionRepo.findByIdForUser(5L, 1L)).thenReturn(Optional.of(existing));

        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> service.updateRecurringTransaction(5L, command(START_DATE, List.of(), 1L, true)));
        verify(recurringTransactionRepo, never()).save(any());
    }

    /**
     * Restart nélkül a megállított tranzakció szerkeszthető, és megállítva marad
     */
    @Test
    void testUpdateRecurringTransactionStoppedWithoutRestart() {
        var existing = existing();
        existing.setStatus(RecurringTransactionStatusEnum.STOPPED);
        when(walletService.getWalletById(1L)).thenReturn(wallet);
        when(recurringTransactionRepo.findByIdForUser(5L, 1L)).thenReturn(Optional.of(existing));

        service.updateRecurringTransaction(5L, command(START_DATE, List.of(), 1L));

        assertEquals(RecurringTransactionStatusEnum.STOPPED, existing.getStatus());
        verify(recurringTransactionRepo).save(existing);
    }

    @Test
    void testUpdateRecurringTransactionNotExisting() {
        when(walletService.getWalletById(1L)).thenReturn(wallet);
        when(recurringTransactionRepo.findByIdForUser(5L, 1L)).thenReturn(Optional.empty());

        assertThrowsExactly(
                EntityNotFoundException.class,
                () -> service.updateRecurringTransaction(5L, command(START_DATE, List.of(), 1L)));
        verify(recurringTransactionRepo, never()).save(any());
        verify(recurringTransactionCategoryRepo, never()).deleteAll(any());
    }

    @Test
    void testUpdateRecurringTransactionNotExistingWallet() {
        when(walletService.getWalletById(1L)).thenThrow(new EntityNotFoundException("Wallet not found"));

        assertThrowsExactly(
                EntityNotFoundException.class,
                () -> service.updateRecurringTransaction(5L, command(START_DATE, List.of(), 1L)));
        verify(recurringTransactionRepo, never()).save(any());
    }

    @Test
    void testListRecurringTransactions() {
        var list = List.of(existing());
        when(recurringTransactionRepo.findAllForUser(1L)).thenReturn(list);

        assertSame(list, service.listRecurringTransactions());
    }

    @Test
    void testGetRecurringTransactionById() {
        var existing = existing();
        when(recurringTransactionRepo.findByIdForUser(5L, 1L)).thenReturn(Optional.of(existing));

        assertSame(existing, service.getRecurringTransactionByIdForActualUser(5L));
    }

    @Test
    void testGetRecurringTransactionByIdNotExisting() {
        when(recurringTransactionRepo.findByIdForUser(5L, 1L)).thenReturn(Optional.empty());

        assertThrowsExactly(EntityNotFoundException.class, () -> service.getRecurringTransactionByIdForActualUser(5L));
    }

    @Test
    void testStopRecurringTransaction() {
        var existing = existing();
        when(recurringTransactionRepo.findByIdForUser(5L, 1L)).thenReturn(Optional.of(existing));

        service.stopRecurringTransaction(5L);

        assertEquals(RecurringTransactionStatusEnum.STOPPED, existing.getStatus());
        verify(recurringTransactionRepo).save(existing);
    }

    @Test
    void testStopRecurringTransactionNotExisting() {
        when(recurringTransactionRepo.findByIdForUser(5L, 1L)).thenReturn(Optional.empty());

        assertThrowsExactly(EntityNotFoundException.class, () -> service.stopRecurringTransaction(5L));
        verify(recurringTransactionRepo, never()).save(any());
    }

    @Test
    void testDeleteRecurringTransaction() {
        var existing = existing();
        when(recurringTransactionRepo.findByIdForUser(5L, 1L)).thenReturn(Optional.of(existing));

        service.deleteRecurringTransaction(5L);

        verify(recurringTransactionRepo).delete(existing);
    }

    @Test
    void testDeleteRecurringTransactionNotExisting() {
        when(recurringTransactionRepo.findByIdForUser(5L, 1L)).thenReturn(Optional.empty());

        assertThrowsExactly(EntityNotFoundException.class, () -> service.deleteRecurringTransaction(5L));
        verify(recurringTransactionRepo, never()).delete(any());
    }

    private RecurringTransactionSaveCommand command(LocalDate startDate, List<Long> categoryIds, Long walletId) {
        return command(startDate, categoryIds, walletId, false);
    }

    private RecurringTransactionSaveCommand command(
            LocalDate startDate, List<Long> categoryIds, Long walletId, boolean isRestartMode) {
        return new RecurringTransactionSaveCommand(
                "Netflix",
                new BigDecimal("-4500"),
                startDate,
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                categoryIds,
                walletId,
                isRestartMode);
    }

    private RecurringTransaction existing() {
        var recurringTransaction = new RecurringTransaction(
                "Netflix",
                new BigDecimal("-4500"),
                START_DATE,
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                wallet);
        recurringTransaction.setId(5L);
        recurringTransaction.setCategoryLinks(List.of());
        return recurringTransaction;
    }

    private Category category(Long id) {
        var category = new Category();
        category.setId(id);
        return category;
    }
}
