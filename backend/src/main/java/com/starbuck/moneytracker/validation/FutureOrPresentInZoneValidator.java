package com.starbuck.moneytracker.validation;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import com.starbuck.moneytracker.util.UserDateUtil;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FutureOrPresentInZoneValidator implements ConstraintValidator<FutureOrPresentInZone, LocalDate> {

    private final Clock clock;
    private final UserDateUtil userDateUtil;

    /** null esetén a user időzónája */
    private ZoneId zone;

    public FutureOrPresentInZoneValidator(Clock clock, UserDateUtil userDateUtil) {
        this.clock = clock;
        this.userDateUtil = userDateUtil;
    }

    @Override
    public void initialize(FutureOrPresentInZone annotation) {
        zone = annotation.zone().isBlank() ? null : ZoneId.of(annotation.zone());
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        LocalDate today = zone == null ? userDateUtil.today() : LocalDate.now(clock.withZone(zone));
        return !value.isBefore(today);
    }
}
