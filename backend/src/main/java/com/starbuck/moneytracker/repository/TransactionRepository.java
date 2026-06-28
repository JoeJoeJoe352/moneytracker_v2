package com.starbuck.moneytracker.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.starbuck.moneytracker.entity.Transaction;

/**
 * Note: Transaction entitásban be van kapcsolva a SQLRestriction, így az autógenerált lekérdezésekben status = 0 szűrést mindig bele fogja tenni
 */
public interface TransactionRepository extends 
        JpaRepository<Transaction, Long>, 
        JpaSpecificationExecutor<Transaction>
{
    /**
     * Visszatér a user összes pénzével. 
     * Lehet null, hogyha még nincs neki tranzakciója
     * 
     * @param userId
     * @return
     */
    @Query("SELECT SUM(t.priceSum) FROM Transaction t WHERE t.user.id = ?1 AND t.status = 0")
    Float summarizeTotalMoneyForUser(long userId); 

    /**
     * Utolsó X darab tranzakcióval tér vissza (id alapján van csökkenő sorrendbe rendezve)
     * TODO átgondolni a feltételt, a pl.: jövő hétre felvett tranzakciók hol jelenjenek meg? Legelején?
     *
     * @param userId
     * @param limit
     * @return
     */
    @Query("SELECT t FROM Transaction t where t.user.id = ?1 AND t.status = 0 ORDER BY t.id DESC LIMIT ?2")
    Transaction[] getLastTransactionsForUserWithLimit(long userId, int limit);

    /**
     * Id alapján lekéri a tranzakciós adatokat
     * 
     * @param userId
     * @param transactionId
     * @return
     */
    @Query("SELECT t FROM Transaction t WHERE t.id = ?1 AND t.user.id = ?2 AND t.status = 0")
    Optional<Transaction> getTransactionById(long transactionId, long userId);
}
