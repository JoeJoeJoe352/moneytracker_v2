package com.starbuck.moneytracker.dto;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record IsEmailExistsRequestDto(
    @NotBlank 
    @Length(min = 5, max = 255, message = "Email must be between 5 and 255 characters")
    @Email 
    String email) {

}
