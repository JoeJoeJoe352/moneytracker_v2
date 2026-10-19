package com.starbuck.moneytracker.dto;

import jakarta.validation.constraints.NotBlank;

public record IsEmailExistsRequestDto(
        @NotBlank
        String email) {

   
}
