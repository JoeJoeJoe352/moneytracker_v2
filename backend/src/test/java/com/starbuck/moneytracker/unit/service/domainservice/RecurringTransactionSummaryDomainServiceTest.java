package com.starbuck.moneytracker.unit.service.domainservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.starbuck.moneytracker.dto.RecurringTransactionMonthlySummaryDto;
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

    // hibás bemenetek

    @Test
    void countPlannedOccurrences_throwsWhenFromIsAfterTo() {
        assertInvalidInput(
                () -> this.service.countPlannedOccurrences(this.recurringTransaction(1L), TO, FROM),
                "from (2026-03-01) cannot be after to (2026-02-01)");
    }

    @Test
    void summarize_throwsWhenCreatedTransactionIsNotFromRecurringTransaction() {
        Transaction manual = this.createdTransaction(7L, null, new BigDecimal("-100"));

        assertInvalidInput(
                () -> this.service.summarizeMonthlyStatistic(
                        MONTH, List.of(manual), List.of(this.recurringTransaction(1L))),
                "Transaction 7 was not created from a recurring transaction");
    }

    /**
     * Ha egy létrejött tranzakció ismétlődő tranzakciója nincs a listában, az
     * összege csendben kimaradna, ezért kivételt dob
     */
    @Test
    void summarize_throwsWhenCreatedTransactionsRecurringTransactionIsMissing() {
        RecurringTransaction listed = this.recurringTransaction(1L);
        RecurringTransaction missing = this.recurringTransaction(2L);
        Transaction created = this.createdTransaction(7L, missing, new BigDecimal("-100"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> this.service.summarizeMonthlyStatistic(MONTH, List.of(created), List.of(listed)));
        assertEquals(
                "Created transactions reference recurring transactions that are missing from the list: [2]",
                exception.getMessage());
    }

    /**
     * Leállított ismétlődő tranzakció priceSum-ját nem használja: nem dob, és
     * létrejött tranzakció nélkül nem is kerül az eredménybe
     */
    @Test
    void summarize_ignoresStoppedRecurringTransactionWithoutCreatedTransactions() {
        RecurringTransaction stopped = this.recurringTransaction(1L);
        stopped.setStatus(RecurringTransactionStatusEnum.STOPPED);
        stopped.setPriceSum(null);

        var result = this.service.summarizeMonthlyStatistic(MONTH, List.of(), List.of(stopped));

        assertTrue(result.isEmpty());
    }

    // summarizeMonthlyStatistic

    @Test
    void summarizeMonthlyStatistic_returnsEmptyListWhenThereAreNoRecurringTransactions() {
        var result = this.service.summarizeMonthlyStatistic(MONTH, List.of(), List.of());

        assertTrue(result.isEmpty());
    }

    /**
     * Csak tervezett: a hónapban még nem jött létre belőle tranzakció. A DTO
     * minden mezőjét ellenőrzi
     */
    @Test
    void summarizeMonthlyStatistic_returnsOnlyPlannedSumWhenNothingWasCreatedYet() {
        RecurringTransaction rent = this.recurringTransaction(1L);

        var result = this.service.summarizeMonthlyStatistic(MONTH, List.of(), List.of(rent));

        assertEquals(1, result.size());
        RecurringTransactionMonthlySummaryDto summary = result.get(0);
        assertEquals(1L, summary.recurringTransactionId());
        assertEquals("Albérlet", summary.name());
        assertAmount("-150000", summary.recurringTransactionPrice());
        assertEquals(FROM.plusDays(9), summary.nextOccurrenceDate());
        assertEquals(TransactionTypeEnum.OUTCOME, summary.transactionType());
        assertAmount("0", summary.createdSum());
        assertAmount("-150000", summary.plannedSum());
        assertEquals(CurrencyEnum.HUF, summary.currencyCode());
    }

    /**
     * A hónapi esedékesség már létrejött, a következő a jövő hónapban van, így
     * nincs
     * tervezett összeg
     */
    @Test
    void summarizeMonthlyStatistic_returnsOnlyCreatedSumWhenThisMonthsOccurrenceWasAlreadyCreated() {
        RecurringTransaction rent = this.recurringTransaction(1L, TO.plusDays(9));
        Transaction created = this.createdTransaction(7L, rent, new BigDecimal("-150000"));

        var result = this.service.summarizeMonthlyStatistic(MONTH, List.of(created), List.of(rent));

        assertEquals(1, result.size());
        assertAmount("-150000", result.get(0).createdSum());
        assertAmount("0", result.get(0).plannedSum());
    }

    /**
     * Heti ismétlődés a hónap közepén: két előfordulás már létrejött (02-02,
     * 02-09), kettő még tervezett (02-16, 02-23)
     */
    @Test
    void summarizeMonthlyStatistic_returnsBothCreatedAndPlannedSums() {
        RecurringTransaction weekly = this.recurringTransaction(1L, LocalDate.of(2026, 2, 16));
        weekly.setStartDate(LocalDate.of(2026, 2, 2));
        weekly.setRecurrenceInterval(RecurrenceIntervalEnum.WEEKLY);
        weekly.setPriceSum(new BigDecimal("-1000"));
        Transaction first = this.createdTransaction(7L, weekly, new BigDecimal("-1000"));
        Transaction second = this.createdTransaction(8L, weekly, new BigDecimal("-1000"));

        var result = this.service.summarizeMonthlyStatistic(MONTH, List.of(first, second), List.of(weekly));

        assertEquals(1, result.size());
        assertAmount("-2000", result.get(0).createdSum());
        assertAmount("-2000", result.get(0).plannedSum());
    }

    /**
     * A létrejött összeg a valós tranzakciókból jön, nem a sablon (azóta
     * módosított) árából
     */
    @Test
    void summarizeMonthlyStatistic_usesRealTransactionAmountForCreatedSum() {
        RecurringTransaction rent = this.recurringTransaction(1L, TO.plusDays(9));
        rent.setPriceSum(new BigDecimal("-160000"));
        Transaction created = this.createdTransaction(7L, rent, new BigDecimal("-150000"));

        var result = this.service.summarizeMonthlyStatistic(MONTH, List.of(created), List.of(rent));

        assertAmount("-150000", result.get(0).createdSum());
        assertAmount("-160000", result.get(0).recurringTransactionPrice());
    }

    /**
     * Leállított ismétlődő tranzakció: a létrejött összeg megjelenik, tervezett
     * nincs, és a következő esedékesség null
     */
    @Test
    void summarizeMonthlyStatistic_returnsCreatedSumButNoPlannedSumForStoppedRecurringTransaction() {
        RecurringTransaction stopped = this.recurringTransaction(1L);
        stopped.setStatus(RecurringTransactionStatusEnum.STOPPED);
        Transaction created = this.createdTransaction(7L, stopped, new BigDecimal("-150000"));

        var result = this.service.summarizeMonthlyStatistic(MONTH, List.of(created), List.of(stopped));

        assertEquals(1, result.size());
        assertAmount("-150000", result.get(0).createdSum());
        assertAmount("0", result.get(0).plannedSum());
        assertNull(result.get(0).nextOccurrenceDate());
    }

    /**
     * Ami ebben a hónapban se létre nem jött, se nem lesz esedékes, nem kerül az
     * eredménybe
     */
    @Test
    void summarizeMonthlyStatistic_excludesRecurringTransactionsWithNothingInTheMonth() {
        RecurringTransaction thisMonth = this.recurringTransaction(1L);
        RecurringTransaction laterMonth = this.recurringTransaction(2L, LocalDate.of(2026, 4, 10));

        var result = this.service.summarizeMonthlyStatistic(MONTH, List.of(), List.of(thisMonth, laterMonth));

        assertEquals(List.of(1L), this.toIds(result));
    }

    /**
     * Ismétlődő tranzakciónként külön összegez, több létrejött tranzakciót is
     * összead
     */
    @Test
    void summarizeMonthlyStatistic_sumsCreatedTransactionsPerRecurringTransaction() {
        RecurringTransaction rent = this.recurringTransaction(1L, TO.plusDays(9));
        RecurringTransaction netflix = this.recurringTransaction(2L, TO.plusDays(9));
        Transaction rentFirst = this.createdTransaction(7L, rent, new BigDecimal("-100000"));
        Transaction netflixOnly = this.createdTransaction(8L, netflix, new BigDecimal("-4000"));
        Transaction rentSecond = this.createdTransaction(9L, rent, new BigDecimal("-50000"));

        var result = this.service.summarizeMonthlyStatistic(
                MONTH, List.of(rentFirst, netflixOnly, rentSecond), List.of(rent, netflix));

        assertEquals(List.of(1L, 2L), this.toIds(result));
        assertAmount("-150000", result.get(0).createdSum());
        assertAmount("-4000", result.get(1).createdSum());
    }

    /**
     * Az eredmény sorrendje a recurringTransactions sorrendje, nem a létrejött
     * tranzakcióké
     */
    @Test
    void summarizeMonthlyStatistic_keepsTheOrderOfRecurringTransactions() {
        RecurringTransaction first = this.recurringTransaction(2L);
        RecurringTransaction second = this.recurringTransaction(1L);
        Transaction createdForSecond = this.createdTransaction(7L, second, new BigDecimal("-150000"));

        var result = this.service.summarizeMonthlyStatistic(MONTH, List.of(createdForSecond), List.of(first, second));

        assertEquals(List.of(2L, 1L), this.toIds(result));
    }

    /**
     * Ha a létrejött tranzakciók összege pontosan 0 és nincs tervezett összeg, a
     * sor kimarad (a szűrő az összegeket nézi, nem azt, hogy volt-e tranzakció)
     */
    @Test
    void summarizeMonthlyStatistic_excludesRowWhenCreatedSumIsExactlyZeroAndNothingIsPlanned() {
        RecurringTransaction rent = this.recurringTransaction(1L, TO.plusDays(9));
        Transaction plus = this.createdTransaction(7L, rent, new BigDecimal("100"));
        Transaction minus = this.createdTransaction(8L, rent, new BigDecimal("-100"));

        var result = this.service.summarizeMonthlyStatistic(MONTH, List.of(plus, minus), List.of(rent));

        assertTrue(result.isEmpty());
    }

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
        return this.recurringTransaction(id, FROM.plusDays(9));
    }

    /**
     * Mint a recurringTransaction(id), de adott következő esedékességgel (pl. ha a
     * hónapi már létrejött)
     */
    private RecurringTransaction recurringTransaction(Long id, LocalDate nextOccurrenceDate) {
        RecurringTransaction recurringTransaction = new RecurringTransaction(
                "Albérlet",
                new BigDecimal("-150000"),
                FROM.plusDays(9),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                new Wallet(1L, "wallet", null, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT));
        recurringTransaction.setId(id);
        recurringTransaction.setNextOccurrenceDate(nextOccurrenceDate);
        return recurringTransaction;
    }

    /**
     * BigDecimal összehasonlítás a scale-től függetlenül (pl. -150000 ==
     * -150000.00)
     *
     * Csak az eltérés irányát adja vissza (pl.: 1: nagyobb az actual, mint az expected)
     */
    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    private List<Long> toIds(List<RecurringTransactionMonthlySummaryDto> summaries) {
        return summaries.stream()
                .map(RecurringTransactionMonthlySummaryDto::recurringTransactionId)
                .toList();
    }

    /**
     * Ismétlődő tranzakcióból létrejött tranzakció (recurringTransaction null
     * esetén
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
