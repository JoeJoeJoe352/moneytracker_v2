package com.starbuck.moneytracker.dto;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Felhasználó adatátviteli objektum.
 */
public record RegisterRequestDto(
        /**
         * Felhasználónév
         */
        @NotBlank(message = "Username is mandatory")
        @Length(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
        String username,

        /**
         * Felhasználó jelszava
         * Max 72 karakter, a Bcrypt limitje
         */
        @NotBlank(message = "Password is mandatory")
        @Length(min = 6, max = 72, message = "Password must be between 6 and 72 characters")
        String password,

        /**
         * Felhasználó email címe
         */
        @Length(min = 5, max = 255, message = "Email must be between 5 and 255 characters")
        @NotBlank(message = "Email is mandatory")
        @Email(message = "Email should be valid")
        String email) {}
