package com.starbuck.moneytracker.exception;

/**
 * Nem található (vagy nem a userhez tartozó) entitás. A GlobalExceptionHandler
 * 404-re fordítja
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }
}
