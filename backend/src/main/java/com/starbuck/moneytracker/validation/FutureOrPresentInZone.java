package com.starbuck.moneytracker.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Mint a @FutureOrPresent, de a "ma" egy adott időzóna szerint értendő.
 * Ha a zone üres, a requesthez tartozó user időzónáját használja (x-time-zone header, UserDateUtil).
 * A null értéket érvényesnek tekinti, ahhoz @NotNull kell.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FutureOrPresentInZoneValidator.class)
public @interface FutureOrPresentInZone {
    String message() default "Date must be in the present or future";

    /** IANA zóna azonosító (pl.: Europe/Budapest), üres esetén a user időzónája */
    String zone() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
