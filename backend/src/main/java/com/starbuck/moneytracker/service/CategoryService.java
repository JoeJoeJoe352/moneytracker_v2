package com.starbuck.moneytracker.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.repository.CategoryRepository;

@Service
public class CategoryService {
    
    @Autowired
    private CategoryRepository categoryRepository;

    public Category createCategory(Category category) {
        // TODO category egyediséget ellenőrizni előbb
        return this.categoryRepository.save(category);
    }

    public List<Category> listCategories() {
        return categoryRepository.findAll();
    }
}
