package com.starbuck.moneytracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.entity.TransactionDetail;

public interface TransactionDetailRepository extends JpaRepository<TransactionDetail, Long> {
    /**
     * Törli a tranzakció összes detailját, egy bulk DELETE-tel. A soft delete-elt
     * tranzakció detailjaira is működik
     *
     * @param transactionId
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM transaction_details d WHERE d.transaction.id = :transactionId")
    void hardDeleteAllByTransactionId(@Param("transactionId") Long transactionId);
}
