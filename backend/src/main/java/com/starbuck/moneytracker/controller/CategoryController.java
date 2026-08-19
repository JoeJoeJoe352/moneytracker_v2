package com.starbuck.moneytracker.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.starbuck.moneytracker.commands.CategoryCreateCommand;
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
    @PostMapping(path = "/category")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponseDto createCategory(@Valid @RequestBody CategoryCreateDto categoryData) {
        var commandModel = new CategoryCreateCommand(categoryData.name());
        var category = categoryService.createCategory(commandModel);
        return mapper.toDto(category);
    }

    /**
     * Kilistázza a user kategóriáit
     * 
     * @return
     */
    @GetMapping(path = "/category")
    public List<CategoryResponseDto> getCategories() {
        // TODO keresést beletenni
        var categories = categoryService.listCategories();
        return mapper.toDtoList(categories);
    }
}
