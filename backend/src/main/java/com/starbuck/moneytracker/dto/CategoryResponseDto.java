package com.starbuck.moneytracker.dto;

public record CategoryResponseDto(
    Long id, 
    String name,
    boolean isLangKey
) {
    
}
