package com.starbuck.moneytracker.exception;

import java.util.HashMap;
import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
 
import tools.jackson.databind.exc.InvalidFormatException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 
     * Ez a handler a @Valid annotációval jelölt mezők validációs hibáit kezeli, és
     * egy map-ben adja vissza a mezőneveket és a hibaüzeneteket.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Nem olvasható request body (pl. hibás JSON, érvénytelen enum érték). A
     * Jackson üzenete belső részleteket tartalmaz (pl. osztálynevek), ezért
     * érvénytelen enum esetén csak az elfogadott értékek listáját adjuk vissza
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof InvalidFormatException invalidFormat && invalidFormat.getTargetType().isEnum()) {
            String acceptedValues = Arrays.toString(invalidFormat.getTargetType().getEnumConstants());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "not one of the values accepted for Enum class: " + acceptedValues));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Malformed request body"));
    }

    /**
     * Path variable / request param nem konvertálható a várt típusra (pl.
     * /transaction/abc Long id helyett)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Invalid value for parameter '" + ex.getName() + "'"));
    }

    /**
     * Nem létező api végpontra érkező kérés
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Endpoint not found: /" + ex.getResourcePath()));
    }

    /**
     * Service/domain rétegben eldobott validációs hibák (pl. rossz tranzakció
     * adatok, foglalt username)
     */
    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    /**
     * Nem található entitásra hivatkozó kérés (pl. másik user tranzakciója, vagy
     * nem létező id)
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    /**
     * Sikertelen bejelentkezés (rossz username vagy jelszó)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getMessage()));
    }

    /**
     * Már létező, egyedinek szánt erőforrás létrehozásának kísérlete (pl.
     * ugyanolyan nevű kategória)
     */
    @ExceptionHandler(CategoryNonUniqueException.class)
    public ResponseEntity<Map<String, String>> handleConflict(CategoryNonUniqueException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
    }

    /**
     * Minden más, fentebb explicit nem kezelt kivétel. A Spring MVC saját
     * kivételei (pl. 405, 415, hiányzó request param) ErrorResponse-t
     * implementálnak és hordozzák a helyes státuszkódot, ezeket nem 500-ként adjuk
     * vissza
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedError(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            String detail = errorResponse.getBody().getDetail();
            return ResponseEntity.status(errorResponse.getStatusCode())
                    .body(Map.of("message", detail != null ? detail : ex.getClass().getSimpleName()));
        }
        logger.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Unexpected error occurred"));
    }
}
