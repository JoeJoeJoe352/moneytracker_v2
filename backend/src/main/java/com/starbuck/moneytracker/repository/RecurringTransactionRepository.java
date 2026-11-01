package com.starbuck.moneytracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.starbuck.moneytracker.entity.RecurringTransaction;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    /**
     * A user adott id-jú ismétlődő tranzakciója (a töröltek nem, az @SQLRestriction miatt)
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
}
