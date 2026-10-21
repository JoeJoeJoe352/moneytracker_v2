package com.starbuck.moneytracker.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.starbuck.moneytracker.commands.CategoryCreateCommand;
import com.starbuck.moneytracker.dto.CategoryCreateRequestDto;
import com.starbuck.moneytracker.dto.CategoryResponseDto;
import com.starbuck.moneytracker.exception.CategoryNonUniqueException;
import com.starbuck.moneytracker.mapper.CategoryMapper;
import com.starbuck.moneytracker.service.CategoryService;

import jakarta.validation.Valid;

@RequestMapping("/category")
@RestController
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryMapper mapper;

    public CategoryController(CategoryService categoryService, CategoryMapper mapper) {
        this.categoryService = categoryService;
        this.mapper = mapper;
    }

    /**
     * Létrehoz egy új kategóriát
     *
     * @param categoryData
     * @throws CategoryNonUniqueException
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponseDto createCategory(@Valid @RequestBody CategoryCreateRequestDto categoryData)
            throws CategoryNonUniqueException {
        var commandModel = new CategoryCreateCommand(categoryData.name());
        var category = categoryService.createCategory(commandModel);
        return mapper.toDto(category);
    }

    /**
     * Kilistázza a user kategóriáit
     *
     * @return
     */
    @GetMapping
    public List<CategoryResponseDto> getCategories() {
        // TODO keresést beletenni
        var categories = categoryService.listCategoriesForUser();
        return mapper.toDtoList(categories);
    }
}
