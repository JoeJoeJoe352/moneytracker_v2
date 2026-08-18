package com.starbuck.moneytracker.exception;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.NonUniqueObjectException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Ez a handler a @Valid annotációval jelölt mezők validációs hibáit kezeli, és egy map-ben adja vissza a mezőneveket és a hibaüzeneteket.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity.badRequest().body(errors);
    }

    // Service/domain rétegben eldobott validációs hibák (pl. rossz tranzakció adatok, foglalt username)
    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    // Nem található entitásra hivatkozó kérés (pl. másik user tranzakciója, vagy nem létező id)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    // Már létező, egyedinek szánt erőforrás létrehozásának kísérlete (pl. ugyanolyan nevű kategória)
    @ExceptionHandler(NonUniqueObjectException.class)
    public ResponseEntity<Map<String, String>> handleConflict(NonUniqueObjectException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
    }
}
