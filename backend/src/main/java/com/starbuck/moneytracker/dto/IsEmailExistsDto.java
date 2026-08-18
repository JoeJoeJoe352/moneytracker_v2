package com.starbuck.moneytracker.dto;

import jakarta.validation.constraints.NotBlank;

public record IsEmailExistsDto(
        @NotBlank
        String email) {

   
}
