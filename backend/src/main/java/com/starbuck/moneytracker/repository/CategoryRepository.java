package com.starbuck.moneytracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.starbuck.moneytracker.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    
}
