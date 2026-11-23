package com.starbuck.moneytracker.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.starbuck.moneytracker.entity.TransferTransactionSide;

public interface TransferTransactionSidesRepository extends JpaRepository<TransferTransactionSide, Long> {

    /**
     * Az adott id-jú átvezetés, ha mindkét oldala aktív. Törléskor az összekötés
     * megmarad, csak a két tranzakció lesz DISABLED, ezért a törölt átvezetést
     * így szűrjük ki
     */
    @Query(
            "SELECT s FROM TransferTransactionSide s JOIN FETCH s.srcTransaction src JOIN FETCH s.dstTransaction dst WHERE s.id = ?1 AND src.status = ACTIVE AND dst.status = ACTIVE")
    Optional<TransferTransactionSide> findActiveById(long id);

    /**
     * Az átvezetés, aminek a megadott tranzakció valamelyik (forrás vagy cél) oldala, ha mindkét
     * oldala aktív
     */
    @Query(
            "SELECT s FROM TransferTransactionSide s JOIN FETCH s.srcTransaction src JOIN FETCH s.dstTransaction dst WHERE (src.id = ?1 OR dst.id = ?1) AND src.status = ACTIVE AND dst.status = ACTIVE")
    Optional<TransferTransactionSide> findActiveBySideTransactionId(long transactionId);
}
