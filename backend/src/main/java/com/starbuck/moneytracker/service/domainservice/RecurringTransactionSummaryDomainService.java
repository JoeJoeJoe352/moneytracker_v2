package com.starbuck.moneytracker.service.domainservice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.starbuck.moneytracker.dto.RecurringTransactionSummaryCreatedInMonthDto;
import com.starbuck.moneytracker.entity.RecurringTransaction;
import com.starbuck.moneytracker.entity.Transaction;

/**
 * Az ismétlődő tranzakciók havi statisztikáját számolja ki.
 * Nincs függősége, sem állapota (az adatokat a hívó tölti be), ezért
 * nem @Service
 */
public final class RecurringTransactionSummaryDomainService {

    /**
     * Ismétlődő tranzakciónként összegzi az adott hónapot, külön a hónapban már
     * létrejött (createdSum) és a hónap végéig még tervezett, le nem állított
     * ismétlődő tranzakcióból várható (plannedSum) tranzakciókat. Ha valamelyik
     * nincs, az összege 0.
     *
     * @param yearMonth
     * @param createdTransactions   a hónapban ismétlődő tranzakcióból létrejött
     *                              tranzakciók, dátum szerint rendezve
     * @param recurringTransactions a user (nem törölt) ismétlődő tranzakciói
     * @return
     */
    public List<RecurringTransactionSummaryCreatedInMonthDto> summarize(
            YearMonth yearMonth,
            List<Transaction> createdTransactions,
            List<RecurringTransaction> recurringTransactions) {
        if (yearMonth == null || createdTransactions == null || recurringTransactions == null) {
            throw new IllegalArgumentException("month, createdTransactions and recurringTransactions cannot be null");
        }

        LocalDate from = yearMonth.atDay(1);
        LocalDate to = yearMonth.plusMonths(1).atDay(1);

        // Az eredmény sorrendje. LinkedHashMap, hogy garantáltan ugyanaz a sorrend
        // legyen minden futás alkalmával (frontend számára)
        Map<Long, RecurringTransaction> templates = new LinkedHashMap<>();
        Map<Long, BigDecimal> createdSums = new HashMap<>();
        Map<Long, BigDecimal> plannedSums = new HashMap<>();

        this.sumCreatedTransactions(createdTransactions, templates, createdSums);
        this.sumPlannedTransactions(recurringTransactions, from, to, templates, plannedSums);

        return templates.values().stream()
                .map(template -> this.toDto(
                        template,
                        createdSums.getOrDefault(template.getId(), BigDecimal.ZERO),
                        plannedSums.getOrDefault(template.getId(), BigDecimal.ZERO)))
                .toList();
    }

    /**
     * Már létrejött tranzakciók összegzése, a valós összeggel, mert a sablon
     * azóta módosulhatott
     *
     * @param createdTransactions
     * @param templates
     * @param sums
     */
    public void sumCreatedTransactions(
            List<Transaction> createdTransactions,
            Map<Long, RecurringTransaction> templates,
            Map<Long, BigDecimal> sums) {
        for (Transaction transaction : createdTransactions) {
            RecurringTransaction recurringTransaction = transaction.getRecurringTransaction();
            templates.put(recurringTransaction.getId(), recurringTransaction);
            sums.merge(recurringTransaction.getId(), transaction.getPriceSum(), BigDecimal::add);
        }
    }

    /**
     * Még létre nem jött, tervezett tranzakciók összegzése, a sablon aktuális
     * összegével
     *
     * @param recurringTransactions
     * @param from
     * @param to
     * @param templates
     * @param sums
     */
    public void sumPlannedTransactions(
            List<RecurringTransaction> recurringTransactions,
            LocalDate from,
            LocalDate to,
            Map<Long, RecurringTransaction> templates,
            Map<Long, BigDecimal> sums) {
        for (RecurringTransaction recurringTransaction : recurringTransactions) {
            if (recurringTransaction.isStopped()) {
                continue;
            }
            int plannedCount = this.countPlannedOccurrences(recurringTransaction, from, to);
            if (plannedCount == 0) {
                continue;
            }
            templates.put(recurringTransaction.getId(), recurringTransaction);
            sums.merge(
                    recurringTransaction.getId(),
                    recurringTransaction.getPriceSum().multiply(BigDecimal.valueOf(plannedCount)),
                    BigDecimal::add);
        }
    }

    /**
     * Megszámolja, hogy a [from, to) intervallumba hány, még létre nem jött
     * előfordulás esik. A nextOccurrenceDate-től indul, így a már létrejötteket
     * nem számolja újra. A from előtti, lemaradt (még le nem generált)
     * előfordulásokat nem számolja bele
     *
     * @param recurringTransaction
     * @param from
     * @param to
     * @return
     */
    public int countPlannedOccurrences(RecurringTransaction recurringTransaction, LocalDate from, LocalDate to) {
        int count = 0;
        LocalDate occurrence = recurringTransaction.getNextOccurrenceDate();
        while (occurrence.isBefore(to)) {
            if (!occurrence.isBefore(from)) {
                count++;
            }
            occurrence = recurringTransaction
                    .getRecurrenceInterval()
                    .calculateNextOccurrence(recurringTransaction.getStartDate(), occurrence);
        }
        return count;
    }

    /**
     * Dto-vá alakítás
     *
     * @param template
     * @param createdSum
     * @param plannedSum
     * @return
     */
    private RecurringTransactionSummaryCreatedInMonthDto toDto(
            RecurringTransaction template, BigDecimal createdSum, BigDecimal plannedSum) {
        return new RecurringTransactionSummaryCreatedInMonthDto(
                template.getId(),
                template.getName(),
                template.getPriceSum(),
                // leállított ismétlődő tranzakcióból nem jön létre több
                template.isStopped() ? null : template.getNextOccurrenceDate(),
                template.getTransactionType(),
                createdSum,
                plannedSum,
                template.getWallet().getCurrencyCode());
    }
}
