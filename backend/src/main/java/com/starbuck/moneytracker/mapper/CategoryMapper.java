package com.starbuck.moneytracker.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.dto.CategoryResponseDto;
import com.starbuck.moneytracker.entity.Category;

@Component
public class CategoryMapper {

    public CategoryResponseDto toDto(Category category) {
        return new CategoryResponseDto(
                category.getId(),
                category.getName());
    }

    public List<CategoryResponseDto> toDtoList(List<Category> entities) {
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
