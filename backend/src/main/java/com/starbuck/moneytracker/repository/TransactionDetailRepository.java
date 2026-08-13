package com.starbuck.moneytracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.TransactionDetail;

public interface TransactionDetailRepository extends JpaRepository<TransactionDetail, Long> {
    
    @Query("SELECT tdc.category FROM TransactionDetailCategory tdc WHERE tdc.transactionDetail.id = :detailId")
    List<Category> findCategoriesByDetailId(@Param("detailId") Long detailId);
}
