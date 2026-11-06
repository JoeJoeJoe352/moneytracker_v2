package com.starbuck.moneytracker.unit.service.domainservice;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.starbuck.moneytracker.entity.RecurringTransaction;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enums.CurrencyEnum;
import com.starbuck.moneytracker.entity.enums.RecurrenceIntervalEnum;
import com.starbuck.moneytracker.entity.enums.RecurringTransactionStatusEnum;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;
import com.starbuck.moneytracker.entity.enums.WalletTypeEnum;
import com.starbuck.moneytracker.service.domainservice.RecurringTransactionSummaryDomainService;

public class RecurringTransactionSummaryDomainServiceTest {

    /** A hibás bemenet tesztek hónapja és intervalluma: [FROM, TO) */
    private static final YearMonth MONTH = YearMonth.of(2026, 2);

    private static final LocalDate FROM = MONTH.atDay(1);
    private static final LocalDate TO = MONTH.plusMonths(1).atDay(1);

    private RecurringTransactionSummaryDomainService service;

    @BeforeEach
    void setUp() {
        this.service = new RecurringTransactionSummaryDomainService();
    }

    /**
     * A to nem része az intervallumnak: [from, to). A summarize a következő hónap
     * első napját adja át to-ként
     */
    @ParameterizedTest(name = "start: {0}, next: {1}, {2}-{3} -> {4}")
    @CsvSource({
        "2026-01-01, 2026-02-01, 2026-02-01, 2026-02-28, 1",
        "2026-01-15, 2026-02-15, 2026-02-01, 2026-03-01, 1",
        "2026-01-31, 2026-01-31, 2026-01-01, 2026-02-01, 1",
        // a to napja már nincs benne
        "2026-01-01, 2026-03-01, 2026-02-01, 2026-03-01, 0",
        "2026-01-10, 2026-04-10, 2026-02-01, 2026-03-01, 0",
        // lemaradt előző havi esedékességet nem számol, csak a hónapon belülit
        "2025-12-15, 2025-12-15, 2026-01-01, 2026-02-01, 1",
        // 31-i kezdés: februárban a hónap utolsó napjára esik
        "2026-01-31, 2026-02-28, 2026-02-01, 2026-03-01, 1",
        // 31-i kezdés: márciusban újra 31-ére esik
        "2026-01-31, 2026-03-31, 2026-03-01, 2026-04-01, 1",
        // 31-i kezdés, három hónapos intervallum: 01-31, 02-28, 03-31
        "2026-01-31, 2026-01-31, 2026-01-01, 2026-04-01, 3",
        // szökőév: februárban 29-ére esik
        "2028-01-31, 2028-01-31, 2028-02-01, 2028-03-01, 1",
        // egész éves intervallum
        "2026-01-10, 2026-01-10, 2026-01-01, 2027-01-01, 12",
        // üres intervallum
        "2026-01-01, 2026-01-01, 2026-01-01, 2026-01-01, 0",
        // március 31.-ei kimarad
        "2026-01-31, 2026-01-31, 2026-01-01, 2026-03-31, 2",
    })
    void testCountPlannedOccurrencesMonthly(
            LocalDate startDate, LocalDate nextOccurenceDate, LocalDate from, LocalDate to, int expectedResult) {
        // Given
        RecurringTransaction rTransaction = new RecurringTransaction();
        rTransaction.setStartDate(startDate);
        rTransaction.setNextOccurrenceDate(nextOccurenceDate);
        rTransaction.setRecurrenceInterval(RecurrenceIntervalEnum.MONTHLY);

        // WHEN
        int result = this.service.countPlannedOccurrences(rTransaction, from, to);
        // THEN
        assertEquals(expectedResult, result);
    }

    @ParameterizedTest(name = "start: {0}, next: {1}, {2}-{3} -> {4}")
    @CsvSource({
        // február: 02-02, 02-09, 02-16, 02-23
        "2026-02-02, 2026-02-02, 2026-02-01, 2026-03-01, 4",
        // március: 03-02, 03-09, 03-16, 03-23, 03-30
        "2026-03-02, 2026-03-02, 2026-03-01, 2026-04-01, 5",
        // hónap közepétől: 02-16, 02-23
        "2026-02-02, 2026-02-16, 2026-02-01, 2026-03-01, 2",
        // lemaradt előző havi (01-26) nem számít, utána 02-02, 02-09, 02-16, 02-23
        "2026-01-26, 2026-01-26, 2026-02-01, 2026-03-01, 4",
        // a to napja már nincs benne
        "2026-02-02, 2026-03-01, 2026-02-01, 2026-03-01, 0",
    })
    void testCountPlannedOccurrencesWeekly(
            LocalDate startDate, LocalDate nextOccurenceDate, LocalDate from, LocalDate to, int expectedResult) {
        // Given
        RecurringTransaction rTransaction = new RecurringTransaction();
        rTransaction.setStartDate(startDate);
        rTransaction.setNextOccurrenceDate(nextOccurenceDate);
        rTransaction.setRecurrenceInterval(RecurrenceIntervalEnum.WEEKLY);

        // WHEN
        int result = this.service.countPlannedOccurrences(rTransaction, from, to);
        // THEN
        assertEquals(expectedResult, result);
    }

    // countPlannedOccurrences - hibás bemenetek

    @Test
    void countPlannedOccurrences_throwsWhenRecurringTransactionIsNull() {
        assertInvalidInput(
                () -> this.service.countPlannedOccurrences(null, FROM, TO), "recurringTransaction cannot be null");
    }

    @Test
    void countPlannedOccurrences_throwsWhenFromIsNull() {
        assertInvalidInput(
                () -> this.service.countPlannedOccurrences(this.recurringTransaction(1L), null, TO),
                "from cannot be null");
    }

    @Test
    void countPlannedOccurrences_throwsWhenToIsNull() {
        assertInvalidInput(
                () -> this.service.countPlannedOccurrences(this.recurringTransaction(1L), FROM, null),
                "to cannot be null");
    }

    @Test
    void countPlannedOccurrences_throwsWhenFromIsAfterTo() {
        assertInvalidInput(
                () -> this.service.countPlannedOccurrences(this.recurringTransaction(1L), TO, FROM),
                "from (2026-03-01) cannot be after to (2026-02-01)");
    }

    @Test
    void countPlannedOccurrences_throwsWhenNextOccurrenceDateIsNull() {
        RecurringTransaction recurringTransaction = this.recurringTransaction(1L);
        recurringTransaction.setNextOccurrenceDate(null);

        assertInvalidInput(
                () -> this.service.countPlannedOccurrences(recurringTransaction, FROM, TO),
                "nextOccurrenceDate cannot be null");
    }

    @Test
    void countPlannedOccurrences_throwsWhenStartDateIsNull() {
        RecurringTransaction recurringTransaction = this.recurringTransaction(1L);
        recurringTransaction.setStartDate(null);

        assertInvalidInput(
                () -> this.service.countPlannedOccurrences(recurringTransaction, FROM, TO), "startDate cannot be null");
    }

    @Test
    void countPlannedOccurrences_throwsWhenRecurrenceIntervalIsNull() {
        RecurringTransaction recurringTransaction = this.recurringTransaction(1L);
        recurringTransaction.setRecurrenceInterval(null);

        assertInvalidInput(
                () -> this.service.countPlannedOccurrences(recurringTransaction, FROM, TO),
                "recurrenceInterval cannot be null");
    }

    // sumCreatedTransactions - hibás bemenetek

    @Test
    void sumCreatedTransactions_throwsWhenListIsNull() {
        assertInvalidInput(
                () -> this.service.sumCreatedTransactions(null, new HashMap<>()), "createdTransactions cannot be null");
    }

    @Test
    void sumCreatedTransactions_throwsWhenSumsIsNull() {
        assertInvalidInput(() -> this.service.sumCreatedTransactions(List.of(), null), "sums cannot be null");
    }

    @Test
    void sumCreatedTransactions_throwsWhenElementIsNull() {
        assertInvalidInput(
                () -> this.service.sumCreatedTransactions(Arrays.asList((Transaction) null), new HashMap<>()),
                "createdTransactions element cannot be null");
    }

    @Test
    void sumCreatedTransactions_throwsWhenTransactionIsNotFromRecurringTransaction() {
        Transaction manual = this.createdTransaction(7L, null, new BigDecimal("-100"));

        assertInvalidInput(
                () -> this.service.sumCreatedTransactions(List.of(manual), new HashMap<>()),
                "Transaction 7 was not created from a recurring transaction");
    }

    @Test
    void sumCreatedTransactions_throwsWhenPriceSumIsNull() {
        Transaction transaction = this.createdTransaction(7L, this.recurringTransaction(1L), null);

        assertInvalidInput(
                () -> this.service.sumCreatedTransactions(List.of(transaction), new HashMap<>()),
                "priceSum of transaction 7 cannot be null");
    }

    // sumPlannedTransactions - hibás bemenetek

    @Test
    void sumPlannedTransactions_throwsWhenCollectionIsNull() {
        assertInvalidInput(
                () -> this.service.sumPlannedTransactions(null, FROM, TO, new HashMap<>()),
                "recurringTransactions cannot be null");
    }

    @Test
    void sumPlannedTransactions_throwsWhenSumsIsNull() {
        assertInvalidInput(() -> this.service.sumPlannedTransactions(List.of(), FROM, TO, null), "sums cannot be null");
    }

    /**
     * Az intervallumot akkor is ellenőrzi, ha a countPlannedOccurrences meg sem
     * hívódna (pl. csak leállított ismétlődő tranzakció van)
     */
    @Test
    void sumPlannedTransactions_throwsWhenFromIsAfterToEvenIfAllAreStopped() {
        RecurringTransaction stopped = this.recurringTransaction(1L);
        stopped.setStatus(RecurringTransactionStatusEnum.STOPPED);

        assertInvalidInput(
                () -> this.service.sumPlannedTransactions(List.of(stopped), TO, FROM, new HashMap<>()),
                "from (2026-03-01) cannot be after to (2026-02-01)");
    }

    @Test
    void sumPlannedTransactions_throwsWhenElementIsNull() {
        assertInvalidInput(
                () -> this.service.sumPlannedTransactions(
                        Arrays.asList((RecurringTransaction) null), FROM, TO, new HashMap<>()),
                "recurringTransactions element cannot be null");
    }

    @Test
    void sumPlannedTransactions_throwsWhenPriceSumIsNullAndItIsPlanned() {
        RecurringTransaction recurringTransaction = this.recurringTransaction(1L);
        recurringTransaction.setPriceSum(null);

        assertInvalidInput(
                () -> this.service.sumPlannedTransactions(List.of(recurringTransaction), FROM, TO, new HashMap<>()),
                "priceSum of recurringTransaction 1 cannot be null");
    }

    /**
     * A priceSum-ot csak akkor ellenőrzi, ha használja: leállított ismétlődő
     * tranzakciónál nem dob
     */
    @Test
    void sumPlannedTransactions_doesNotThrowWhenPriceSumIsNullButItIsStopped() {
        RecurringTransaction stopped = this.recurringTransaction(1L);
        stopped.setStatus(RecurringTransactionStatusEnum.STOPPED);
        stopped.setPriceSum(null);
        Map<Long, BigDecimal> sums = new HashMap<>();

        assertDoesNotThrow(() -> this.service.sumPlannedTransactions(List.of(stopped), FROM, TO, sums));
        assertTrue(sums.isEmpty());
    }

    // summarize - hibás bemenetek

    @Test
    void summarize_throwsWhenYearMonthIsNull() {
        assertInvalidInput(() -> this.service.summarize(null, List.of(), List.of()), "yearMonth cannot be null");
    }

    @Test
    void summarize_throwsWhenCreatedTransactionsIsNull() {
        assertInvalidInput(() -> this.service.summarize(MONTH, null, List.of()), "createdTransactions cannot be null");
    }

    @Test
    void summarize_throwsWhenRecurringTransactionsIsNull() {
        assertInvalidInput(
                () -> this.service.summarize(MONTH, List.of(), null), "recurringTransactions cannot be null");
    }

    @Test
    void summarize_throwsWhenRecurringTransactionElementIsNull() {
        assertInvalidInput(
                () -> this.service.summarize(MONTH, List.of(), Arrays.asList((RecurringTransaction) null)),
                "recurringTransactions element cannot be null");
    }

    @Test
    void summarize_throwsWhenRecurringTransactionIdIsNull() {
        RecurringTransaction unsaved = this.recurringTransaction(null);

        assertInvalidInput(
                () -> this.service.summarize(MONTH, List.of(), List.of(unsaved)),
                "recurringTransaction id cannot be null");
    }

    /**
     * Ha egy létrejött tranzakció ismétlődő tranzakciója nincs a listában, az
     * összege csendben kimaradna, ezért kivételt dob
     */
    @Test
    void summarize_throwsWhenCreatedTransactionsTemplateIsMissing() {
        RecurringTransaction listed = this.recurringTransaction(1L);
        RecurringTransaction missing = this.recurringTransaction(2L);
        Transaction created = this.createdTransaction(7L, missing, new BigDecimal("-100"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> this.service.summarize(MONTH, List.of(created), List.of(listed)));
        assertEquals(
                "Created transactions reference recurring transactions that are missing from the templates: [2]",
                exception.getMessage());
    }

    @Test
    void testSumCreatedTransactions() {}

    @Test
    void testSumPlannedTransactions() {}

    @Test
    void testSummarize() {}

    /**
     * Ellenőrzi, hogy IllegalArgumentException-t dob a megadott üzenettel
     */
    private static void assertInvalidInput(Executable executable, String expectedMessage) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals(expectedMessage, exception.getMessage());
    }

    /**
     * Érvényes, aktív, havi ismétlődő tranzakció, aminek a FROM-TO intervallumban
     * van esedékessége
     */
    private RecurringTransaction recurringTransaction(Long id) {
        RecurringTransaction recurringTransaction = new RecurringTransaction(
                "Albérlet",
                new BigDecimal("-150000"),
                FROM.plusDays(9),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                new Wallet(1L, "wallet", null, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT));
        recurringTransaction.setId(id);
        return recurringTransaction;
    }

    /**
     * Ismétlődő tranzakcióból létrejött tranzakció (recurringTransaction null esetén
     * kézzel felvett)
     */
    private Transaction createdTransaction(Long id, RecurringTransaction recurringTransaction, BigDecimal price) {
        Transaction transaction =
                new Transaction("Albérlet", FROM.plusDays(9), TransactionTypeEnum.OUTCOME, price, null);
        transaction.setId(id);
        transaction.setRecurringTransaction(recurringTransaction);
        return transaction;
    }
}
