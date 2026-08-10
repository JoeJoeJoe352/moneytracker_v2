package com.starbuck.moneytracker.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.starbuck.moneytracker.dto.CategoryCreateDto;
import com.starbuck.moneytracker.dto.CategoryResponseDto;
import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.mapper.CategoryMapper;
import com.starbuck.moneytracker.service.CategoryService;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

@RestController
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryMapper mapper;

    /**
     * Létrehoz egy új kategóriát
     * 
     * @param categoryData
     */
    @PostMapping(path = "/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public void createCategory(@Valid @RequestBody CategoryCreateDto categoryData) {
        var model = new Category();
        model.setName(categoryData.name());
        categoryService.createCategory(model);
    }

    /**
     * Kilistázza a user kategóriáit
     * 
     * @return
     */
    @GetMapping(path = "/categories")
    public List<CategoryResponseDto> getCategories() {
        // TODO keresést beletenni
        var categories = categoryService.listCategories();
        return mapper.toDtoList(categories);
    }
}
