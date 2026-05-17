package com.starbuck.moneytracker.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Felhasználó adatátviteli objektum, a regisztrációhoz szükséges adatokkal.
 */
public class UserDto {
    
    @NotNull
    @NotEmpty
    /**
     * Felhasználónév
     */
    private String username;

    @NotNull
    @NotEmpty
    /**
     * Felhasználó jelszava
     */
    private String password;

    @NotNull
    @NotEmpty
    /**
     * Felhasználó jelszava újból, az egyezés vizsgálatához
     */
    private String passwordAgain;

    @NotNull
    @NotEmpty
    /**
     * Felhasználó email címe
     */
    private String email;

    public UserDto() {
    }

    public UserDto(String username, String password, String passwordAgain, String email) {
        this.username = username;
        this.password = password;
        this.passwordAgain = passwordAgain;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordAgain() {
        return passwordAgain;
    }

    public void setPasswordAgain(String passwordAgain) {
        this.passwordAgain = passwordAgain;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
