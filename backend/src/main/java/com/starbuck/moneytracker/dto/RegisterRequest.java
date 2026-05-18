package com.starbuck.moneytracker.dto;

import org.hibernate.validator.constraints.Length;
import com.starbuck.moneytracker.validation.PasswordMatches;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Felhasználó adatátviteli objektum.
 */
@PasswordMatches
public record RegisterRequest(
    /**
     * Felhasználónév
     */
    @NotBlank(message = "Username is mandatory")
    @Length(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    String username,

    /**
     * Felhasználó jelszava
     */
    @NotBlank(message = "Password is mandatory")
    @Length(min = 6, max = 100, message = "Password must be between 6 and 100 characters") // többi validáció frontenden lesz
    String password,

    /**
     * Felhasználó jelszava újból, az egyezés vizsgálatához
     */
    @NotBlank(message = "Password confirmation is mandatory")
    String passwordAgain,

    /**
     * Felhasználó email címe
     */
    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    String email
) {
    // Konstruktor, getterek, setterek automatikusan generálódnak a record miatt
}