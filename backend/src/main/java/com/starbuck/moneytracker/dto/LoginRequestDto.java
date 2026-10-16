package com.starbuck.moneytracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
    
    @NotBlank(message = "Username must not be blank")
    @Email 
    String email,

    @NotBlank(message = "Password must not be blank")
    String password
) {}