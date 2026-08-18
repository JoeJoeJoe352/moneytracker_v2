package com.starbuck.moneytracker.service;

import java.util.List;

import org.hibernate.NonUniqueObjectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.repository.CategoryRepository;
import com.starbuck.moneytracker.util.CurrentUserUtil;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    CurrentUserUtil currentUser;

    /**
     * Létrehoz egy új kategóriát a user számára
     * 
     * @param category
     * @return
     */
    public Category createCategory(Category category) {
        if (category == null || category.getName() == null) {
            throw new IllegalArgumentException("Category is null");
        }

        if (this.categoryRepository.isUserHaveThisCategoryName(category.getName(), currentUser.getUser().getId())) {
            throw new NonUniqueObjectException("Category with this name already exists for the user", null,
                    category.getName());
        }

        category.setUser(currentUser.getUser());

        return this.categoryRepository.save(category);
    }

    /**
     * Kilistázza a közös és a user saját kategóriáit
     * 
     * @return
     */
    public List<Category> listCategories() {
        return categoryRepository.findAllForUser(currentUser.getUser().getId());
    }
}
