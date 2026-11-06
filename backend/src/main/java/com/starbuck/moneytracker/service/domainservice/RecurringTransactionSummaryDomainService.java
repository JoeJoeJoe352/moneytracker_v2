package com.starbuck.moneytracker.service.domainservice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
     * nincs, az összege 0. Csak azok az ismétlődő tranzakciók szerepelnek,
     * amelyekből
     * a hónapban létrejött vagy még létre fog jönni tranzakció, a
     * recurringTransactions
     * sorrendjében
     *
     * @param yearMonth
     * @param createdTransactions   a hónapban ismétlődő tranzakcióból létrejött
     *                              tranzakciók. Mindegyik ismétlődő tranzakciójának
     *                              szerepelnie kell a recurringTransactions-ben
     * @param recurringTransactions a user (nem törölt) ismétlődő tranzakciói
     * @return
     * @throws IllegalArgumentException, ha valamelyik paraméter, elem vagy
     *                                   ismétlődő tranzakció id null
     * @throws IllegalStateException,    ha egy létrejött tranzakció ismétlődő
     *                                   tranzakciója hiányzik a
     *                                   recurringTransactions-ből
     */
    public List<RecurringTransactionSummaryCreatedInMonthDto> summarize(
            YearMonth yearMonth,
            List<Transaction> createdTransactions,
            List<RecurringTransaction> recurringTransactions) {
        requireNonNull(yearMonth, "yearMonth");
        requireNonNull(createdTransactions, "createdTransactions");
        requireNonNull(recurringTransactions, "recurringTransactions");
        for (RecurringTransaction recurringTransaction : recurringTransactions) {
            requireNonNull(recurringTransaction, "recurringTransactions element");
            // a templates kulcsa, null id-val (el nem mentett entitás) nem azonosítható
            requireNonNull(recurringTransaction.getId(), "recurringTransaction id");
        }

        LocalDate from = yearMonth.atDay(1);
        LocalDate to = yearMonth.plusMonths(1).atDay(1);

        // Az eredmény sorrendje a recurringTransactions sorrendje. LinkedHashMap, hogy
        // garantáltan ugyanaz a sorrend legyen minden futás alkalmával (frontend
        // számára)
        Map<Long, RecurringTransaction> templates = recurringTransactions.stream()
                .collect(Collectors.toMap(
                        RecurringTransaction::getId,
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new));
        Map<Long, BigDecimal> createdSums = new HashMap<>();
        Map<Long, BigDecimal> plannedSums = new HashMap<>();

        this.sumCreatedTransactions(createdTransactions, createdSums);
        this.validateAllCreatedHaveTemplate(createdSums, templates);
        this.sumPlannedTransactions(templates.values(), from, to, plannedSums);

        // csak azok, amelyekből a hónapban létrejött vagy még létre fog jönni
        // tranzakció
        return templates.values().stream()
                .filter(template ->
                        createdSums.containsKey(template.getId()) || plannedSums.containsKey(template.getId()))
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
     * @param sums
     * @throws IllegalArgumentException, ha valamelyik paraméter vagy elem null,
     *                                   vagy egy tranzakció nem ismétlődő
     *                                   tranzakcióból jött létre
     */
    public void sumCreatedTransactions(List<Transaction> createdTransactions, Map<Long, BigDecimal> sums) {
        requireNonNull(createdTransactions, "createdTransactions");
        requireNonNull(sums, "sums");

        for (Transaction transaction : createdTransactions) {
            requireNonNull(transaction, "createdTransactions element");
            if (transaction.getRecurringTransaction() == null) {
                throw new IllegalArgumentException(
                        "Transaction " + transaction.getId() + " was not created from a recurring transaction");
            }
            requireNonNull(transaction.getPriceSum(), "priceSum of transaction " + transaction.getId());

            sums.merge(transaction.getRecurringTransaction().getId(), transaction.getPriceSum(), BigDecimal::add);
        }
    }

    /**
     * Ellenőrzi, hogy minden létrejött tranzakció ismétlődő tranzakciója szerepel-e
     * a templates-ben. Ha nem, a létrejött összeg csendben kimaradna az
     * eredményből.
     * A tervezett összegeket nem kell ellenőrizni, mert azok a templates-ből
     * készülnek
     *
     * @param createdSums
     * @param templates
     * @throws IllegalStateException, ha valamelyik hiányzik
     */
    private void validateAllCreatedHaveTemplate(
            Map<Long, BigDecimal> createdSums, Map<Long, RecurringTransaction> templates) {
        List<Long> missingIds = createdSums.keySet().stream()
                .filter(id -> !templates.containsKey(id))
                .sorted()
                .toList();
        if (!missingIds.isEmpty()) {
            throw new IllegalStateException(
                    "Created transactions reference recurring transactions that are missing from the templates: "
                            + missingIds);
        }
    }

    /**
     * Még létre nem jött, tervezett tranzakciók összegzése, a sablon aktuális
     * összegével
     *
     * @param recurringTransactions
     * @param from
     * @param to
     * @param sums
     * @throws IllegalArgumentException, ha valamelyik paraméter vagy elem null,
     *                                   vagy a from a to után van
     */
    public void sumPlannedTransactions(
            Collection<RecurringTransaction> recurringTransactions,
            LocalDate from,
            LocalDate to,
            Map<Long, BigDecimal> sums) {
        requireNonNull(recurringTransactions, "recurringTransactions");
        requireNonNull(sums, "sums");
        // itt is ellenőrizzük, mert csupa leállított ismétlődő tranzakciónál a
        // countPlannedOccurrences meg sem hívódna
        validateRange(from, to);

        for (RecurringTransaction recurringTransaction : recurringTransactions) {
            requireNonNull(recurringTransaction, "recurringTransactions element");
            if (recurringTransaction.isStopped()) {
                continue;
            }
            int plannedCount = this.countPlannedOccurrences(recurringTransaction, from, to);
            if (plannedCount == 0) {
                continue;
            }
            requireNonNull(
                    recurringTransaction.getPriceSum(),
                    "priceSum of recurringTransaction " + recurringTransaction.getId());
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
     * @throws IllegalArgumentException, ha valamelyik paraméter, vagy az ismétlődő
     *                                   tranzakció dátumai / intervalluma null,
     *                                   vagy a from a to után van
     */
    public int countPlannedOccurrences(RecurringTransaction recurringTransaction, LocalDate from, LocalDate to) {
        requireNonNull(recurringTransaction, "recurringTransaction");
        validateRange(from, to);
        requireNonNull(recurringTransaction.getNextOccurrenceDate(), "nextOccurrenceDate");
        requireNonNull(recurringTransaction.getStartDate(), "startDate");
        requireNonNull(recurringTransaction.getRecurrenceInterval(), "recurrenceInterval");

        int count = 0;
        LocalDate occurrence = recurringTransaction.getNextOccurrenceDate();
        while (occurrence.isBefore(to)) {
            boolean isNextOccurenceAfterFromDate = !occurrence.isBefore(from);
            if (isNextOccurenceAfterFromDate) {
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

    /**
     * @throws IllegalArgumentException, ha a value null
     */
    private static void requireNonNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }

    /**
     * Ellenőrzi a [from, to) intervallumot. from == to megengedett (üres
     * intervallum)
     *
     * @throws IllegalArgumentException, ha valamelyik null, vagy a from a to után
     *                                   van
     */
    private static void validateRange(LocalDate from, LocalDate to) {
        requireNonNull(from, "from");
        requireNonNull(to, "to");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from (" + from + ") cannot be after to (" + to + ")");
        }
    }
}
