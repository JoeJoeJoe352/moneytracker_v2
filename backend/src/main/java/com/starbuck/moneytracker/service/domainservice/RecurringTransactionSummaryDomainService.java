package com.starbuck.moneytracker.service.domainservice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.starbuck.moneytracker.dto.RecurringTransactionMonthlySummaryDto;
import com.starbuck.moneytracker.entity.RecurringTransaction;
import com.starbuck.moneytracker.entity.Transaction;

/**
 * Az ismétlődő tranzakciók havi statisztikáját számolja ki.
 * Nincs függősége, sem állapota (az adatokat a hívó tölti be), ezért
 * nem @Service
 */
public final class RecurringTransactionSummaryDomainService {

    /**
     * Ismétlődő tranzakciónként összegzi a hónapot: külön a hónapban már létrejött
     * (createdSum) és a hónap végéig még tervezett (plannedSum) tranzakciókat.
     * Csak azok szerepelnek, amelyeknek van létrejött vagy tervezett összege, a
     * recurringTransactions sorrendjében.
     *
     * @param month                 a statisztika hónapja
     * @param createdTransactions   a hónapban ismétlődő tranzakcióból létrejött
     *                              tranzakciók
     * @param recurringTransactions a user (nem törölt) ismétlődő tranzakciói
     * @throws IllegalArgumentException ha egy tranzakció nem ismétlődő
     *                                  tranzakcióból jött létre
     * @throws IllegalStateException    ha egy létrejött tranzakció ismétlődő
     *                                  tranzakciója
     *                                  hiányzik a recurringTransactions-ből
     */
    public List<RecurringTransactionMonthlySummaryDto> summarizeMonthlyStatistic(
            YearMonth month, List<Transaction> createdTransactions, List<RecurringTransaction> recurringTransactions) {
        Map<Long, BigDecimal> priceSumByRecurringTransactionId =
                this.sumTransactionPriceByRecurringTransactionId(createdTransactions);
        this.validateAllCreatedHaveRecurringTransaction(priceSumByRecurringTransactionId, recurringTransactions);

        return recurringTransactions.stream()
                .map(recurringTransaction -> this.toDto(
                        recurringTransaction,
                        priceSumByRecurringTransactionId.getOrDefault(recurringTransaction.getId(), BigDecimal.ZERO),
                        this.plannedSum(recurringTransaction, month)))
                .filter(summary -> summary.createdSum().signum() != 0
                        || summary.plannedSum().signum() != 0)
                .toList();
    }

    /**
     * Megszámolja, hogy a [from, to) intervallumba hány, még létre nem jött
     * előfordulás esik. A nextOccurrenceDate-től indul, így a már létrejötteket
     * nem számolja újra. A from előtti, lemaradt (még le nem generált)
     * előfordulásokat nem számolja bele.
     *
     * @throws IllegalArgumentException ha a from a to után van
     */
    public int countPlannedOccurrences(RecurringTransaction recurringTransaction, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from (" + from + ") cannot be after to (" + to + ")");
        }

        int count = 0;
        LocalDate occurrence = recurringTransaction.getNextOccurrenceDate();
        while (occurrence.isBefore(to)) {
            boolean isInRange = !occurrence.isBefore(from);
            if (isInRange) {
                count++;
            }
            occurrence = recurringTransaction
                    .getRecurrenceInterval()
                    .calculateNextOccurrence(recurringTransaction.getStartDate(), occurrence);
        }
        return count;
    }

    /**
     * A létrejött tranzakciók összege ismétlődő tranzakciónként. A valós összeget
     * használja, mert a sablon azóta módosulhatott.
     */
    private Map<Long, BigDecimal> sumTransactionPriceByRecurringTransactionId(List<Transaction> createdTransactions) {
        Map<Long, BigDecimal> sums = new HashMap<>();
        for (Transaction transaction : createdTransactions) {
            if (transaction.getRecurringTransaction() == null) {
                throw new IllegalArgumentException(
                        "Transaction " + transaction.getId() + " was not created from a recurring transaction");
            }
            sums.merge(transaction.getRecurringTransaction().getId(), transaction.getPriceSum(), BigDecimal::add);
        }
        return sums;
    }

    /**
     * Ha egy létrejött tranzakció ismétlődő tranzakciója nincs a listában, az
     * összege csendben kimaradna az eredményből, ezért kivételt dob.
     */
    private void validateAllCreatedHaveRecurringTransaction(
            Map<Long, BigDecimal> priceSumByRecurringTransactionId, List<RecurringTransaction> recurringTransactions) {
        Set<Long> knownRecurringTransactionIds =
                recurringTransactions.stream().map(RecurringTransaction::getId).collect(Collectors.toSet());

        Set<Long> recurringTransactionIds = priceSumByRecurringTransactionId.keySet();
        List<Long> missingIds = recurringTransactionIds.stream()
                .filter(id -> !knownRecurringTransactionIds.contains(id))
                .toList();
        if (!missingIds.isEmpty()) {
            throw new IllegalStateException(
                    "Created transactions reference recurring transactions that are missing from the list: "
                            + missingIds);
        }
    }

    /**
     * A hónap végéig még tervezett tranzakciók összege, a sablon aktuális
     * összegével.
     * Leállított ismétlődő tranzakcióból már nem jön létre több.
     */
    private BigDecimal plannedSum(RecurringTransaction recurringTransaction, YearMonth month) {
        if (recurringTransaction.isStopped()) {
            return BigDecimal.ZERO;
        }
        int plannedCount = this.countPlannedOccurrences(
                recurringTransaction, month.atDay(1), month.plusMonths(1).atDay(1));
        if (plannedCount == 0) {
            return BigDecimal.ZERO;
        }
        return recurringTransaction.getPriceSum().multiply(BigDecimal.valueOf(plannedCount));
    }

    private RecurringTransactionMonthlySummaryDto toDto(
            RecurringTransaction recurringTransaction, BigDecimal createdSum, BigDecimal plannedSum) {
        return new RecurringTransactionMonthlySummaryDto(
                recurringTransaction.getId(),
                recurringTransaction.getName(),
                recurringTransaction.getPriceSum(),
                // leállított ismétlődő tranzakcióból nem jön létre több
                recurringTransaction.isStopped() ? null : recurringTransaction.getNextOccurrenceDate(),
                recurringTransaction.getTransactionType(),
                createdSum,
                plannedSum,
                recurringTransaction.getWallet().getCurrencyCode());
    }
}
