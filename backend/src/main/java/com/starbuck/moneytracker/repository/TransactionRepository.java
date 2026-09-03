package com.starbuck.moneytracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.dto.HistoryQueryHelperDto;
import com.starbuck.moneytracker.dto.WalletSummaryDto;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;
import com.starbuck.moneytracker.util.TransactionSpecifications;

/**
 * Note: Transaction entitásban be van kapcsolva a SQLRestriction, így az
 * autógenerált lekérdezésekben status = 0 szűrést mindig bele fogja tenni
 */
public interface TransactionRepository extends
        JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    /**
     * User számára kilistázza a tranzakcióit
     * 
     * @param userId
     * @param specifications
     * @return
     */
    default List<Transaction> findAllForUser(Long userId, HistoryQueryHelperDto specifications) {
        Specification<Transaction> finalSpec = Specification
                .where(TransactionSpecifications.hasUserId(userId));

        if (specifications.spec() != null) {
            finalSpec = finalSpec.and(specifications.spec());
        }

        return findAll(finalSpec, PageRequest.of(0, specifications.limit(), specifications.sort())).getContent();
    }

    /**
     * Kiszámolja a tranzakciók alapján, hogy jelenlegi hónapban mennyi kiadás volt
     * 
     * @param userId
     * @return
     */
    @Query("""
                SELECT new com.starbuck.moneytracker.dto.WalletSummaryDto(
                    w.currencyCode,
                    COALESCE(SUM(t.priceSum), 0)
                )
                FROM Wallet w
                LEFT JOIN w.transactions t ON
                    YEAR(t.transactionDate) = YEAR(CURDATE()) AND
                    MONTH(t.transactionDate) = MONTH(CURDATE()) AND
                    t.transactionType = ?2
                WHERE w.user.id = ?1
                GROUP BY w.id
                ORDER BY w.id
            """)
    List<WalletSummaryDto> summarizeTransactionPricesForMonthAndType(long userId, TransactionTypeEnum type);

    /**
     * Id alapján lekéri a tranzakciós adatokat
     * 
     * @param userId
     * @param transactionId
     * @return
     */
    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.transactionDetails WHERE t.id = ?1 AND t.wallet.user.id = ?2 AND t.status = 0")
    Optional<Transaction> getTransactionByIdWithDetails(long transactionId, long userId);

    /**
     * Lekéri az összes tranzakciót a tranzakció részletekkel együtt
     * JOIN FETCH, mert alapból lazy a tranzakció részletek betöltése, így a
     * tranzakció részletek nem lennének elérhetőek
     * TODO ha sok az elem, akkor gond lehet a sok lekérésből (elfogy a mem)
     * 
     * @return
     */
    @Query("SELECT t FROM Transaction t JOIN FETCH t.transactionDetails td WHERE t.status = 0 ")
    List<Transaction> getAllTransaction();

    /**
     * Alapból a tranzakciók softdelete-el törlődnek, ez a hard delete-et teszi
     * lehetővé
     * 
     * @param id
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM transactions WHERE id = :id", nativeQuery = true)
    void hardDeleteTransaction(@Param("id") Long id);
}
