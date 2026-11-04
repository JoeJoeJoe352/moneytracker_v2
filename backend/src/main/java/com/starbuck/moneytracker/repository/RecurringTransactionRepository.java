package com.starbuck.moneytracker.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.starbuck.moneytracker.entity.RecurringTransaction;

import jakarta.persistence.LockModeType;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    /**
     * A user adott id-jú ismétlődő tranzakciója (a töröltek nem, az @SQLRestriction
     * miatt)
     *
     * @param id
     * @param userId
     * @return
     */
    @Query("SELECT r FROM RecurringTransaction r WHERE r.id = ?1 AND r.wallet.user.id = ?2")
    Optional<RecurringTransaction> findByIdForUser(long id, long userId);

    /**
     * A user összes ismétlődő tranzakciója, a legközelebb esedékes elöl
     *
     * @param userId
     * @return
     */
    @Query(
            "SELECT r FROM RecurringTransaction r WHERE r.wallet.user.id = ?1 ORDER BY r.nextOccurrenceDate ASC, r.id ASC")
    List<RecurringTransaction> findAllForUser(long userId);

    /**
     * Összes user adott napig (azt is beleértve) esedékes, aktív ismétlődő
     * tranzakcióinak id-jai
     */
    @Query("""
            SELECT r.id FROM RecurringTransaction r
            WHERE r.nextOccurrenceDate <= ?1 AND r.status = 'ACTIVE' AND r.wallet.status = 'ACTIVE'
            ORDER BY r.nextOccurrenceDate ASC, r.id ASC
            """)
    List<Long> findDueTransactionIds(LocalDate date);

    /**
     * Adott id-jú ismétlődő tranzakció, ha még mindig esedékes és aktív. A
     * tranzakció végéig zárolja, hogy párhuzamos futás vagy a user módosítása ne
     * ütközzön vele. Csak @Transactional metóduson belülről hívható.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM RecurringTransaction r
            LEFT JOIN FETCH r.categoryLinks
            WHERE r.id = ?1 AND r.nextOccurrenceDate <= ?2 AND r.status = 'ACTIVE' AND r.wallet.status = 'ACTIVE'
            """)
    Optional<RecurringTransaction> findDueRecurringTransactionByIdForUpdate(long id, LocalDate date);
}
