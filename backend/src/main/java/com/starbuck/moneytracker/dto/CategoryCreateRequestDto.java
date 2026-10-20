package com.starbuck.moneytracker.dto;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;

public record CategoryCreateRequestDto(
        @NotBlank @Length(min = 3, max = 30, message = "Category name must be between 3 and 30 characters")
        String name) {}
