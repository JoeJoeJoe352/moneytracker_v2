package com.starbuck.moneytracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.dto.WalletSummaryResponseDto;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;
import com.starbuck.moneytracker.util.HistoryQueryHelper;
import com.starbuck.moneytracker.util.TransactionSpecifications;

/**
 * Note: Transaction entitásban be van kapcsolva a SQLRestriction, így az
 * autógenerált lekérdezésekben status = 'ACTIVE' szűrést mindig bele fogja
 * tenni
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    /**
     * Kilistázza a user, az adott feltételeknek megfelelő tranzakcióit
     *
     * @param userId
     * @param specifications
     * @return
     */
    default List<Transaction> findAllForUser(Long userId, HistoryQueryHelper specifications) {
        Specification<Transaction> finalSpec = Specification.where(TransactionSpecifications.hasUserId(userId));

        if (specifications.spec() != null) {
            finalSpec = finalSpec.and(specifications.spec());
        }

        return findAll(
                        finalSpec,
                        PageRequest.of(specifications.pageNumber(), specifications.limit(), specifications.sort()))
                .getContent();
    }

    /**
     * Visszatér a user, az adott feltételeknek megfelelő tranzakcióinak a számával
     *
     * @param userId
     * @param specifications
     * @return
     */
    default long countAllForUser(Long userId, Specification<Transaction> specifications) {
        Specification<Transaction> finalSpec = Specification.where(TransactionSpecifications.hasUserId(userId));

        if (specifications != null) {
            finalSpec = finalSpec.and(specifications);
        }

        return count(finalSpec);
    }

    /**
     * Kiszámolja a tranzakciók alapján, hogy jelenlegi hónapban mennyi kiadás volt
     *
     * @param userId
     * @return
     */
    @Query("""
                SELECT new com.starbuck.moneytracker.dto.WalletSummaryResponseDto(
                    w.currencyCode,
                    COALESCE(SUM(t.priceSum), 0)
                )
                FROM Wallet w
                LEFT JOIN w.transactions t ON
                    YEAR(t.transactionDate) = YEAR(CURDATE()) AND
                    MONTH(t.transactionDate) = MONTH(CURDATE()) AND
                    t.transactionType = ?2
                WHERE w.user.id = ?1 AND w.status = ACTIVE
                GROUP BY w.id
                ORDER BY w.id
            """)
    List<WalletSummaryResponseDto> summarizeTransactionPricesForMonthAndType(long userId, TransactionTypeEnum type);

    /**
     * Id alapján lekéri a tranzakciós adatokat
     *
     * @param userId
     * @param transactionId
     * @return
     */
    @Query(
            "SELECT t FROM Transaction t LEFT JOIN FETCH t.transactionDetails WHERE t.id = ?1 AND t.wallet.user.id = ?2 AND t.status = ACTIVE")
    Optional<Transaction> getTransactionByIdWithDetails(long transactionId, long userId);

    /**
     * Az aktív tranzakciók azonosítói id szerint növekvő sorrendben, a megadott
     * id utántól (keyset lapozás: a következő lapot az előző lap utolsó id-ja
     * után kéri le, így nem kell az egész táblát memóriába tölteni)
     *
     * @param lastId az előző lap utolsó id-ja (első lapnál 0)
     * @param limit  lapméret
     * @return
     */
    @Query("SELECT t.id FROM Transaction t WHERE t.status = ACTIVE AND t.id > ?1 ORDER BY t.id")
    List<Long> getActiveTransactionIdsAfter(long lastId, Limit limit);

    /**
     * Lekéri a megadott tranzakciókat a tranzakció részletekkel együtt.
     * JOIN FETCH, mert alapból lazy a tranzakció részletek betöltése. LEFT,
     * hogy a részlet nélküli tranzakciók is benne legyenek. DISTINCT, hogy
     * a több részletes tranzakciók ne szerepeljenek többször.
     *
     * @param ids
     * @return
     */
    @Query("SELECT DISTINCT t FROM Transaction t LEFT JOIN FETCH t.transactionDetails WHERE t.id IN ?1")
    List<Transaction> getTransactionsWithDetailsByIds(List<Long> ids);

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
