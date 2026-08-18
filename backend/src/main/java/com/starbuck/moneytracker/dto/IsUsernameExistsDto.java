package com.starbuck.moneytracker.dto;

import jakarta.validation.constraints.NotBlank;

public record IsUsernameExistsDto(
        @NotBlank
        String username) {

}
