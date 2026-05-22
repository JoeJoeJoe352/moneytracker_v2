package com.starbuck.moneytracker.dto;

public record LoginRequest(
    String username,
    String password
) {}