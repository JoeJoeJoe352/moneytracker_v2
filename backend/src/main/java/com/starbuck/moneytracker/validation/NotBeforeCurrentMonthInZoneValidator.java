package com.starbuck.moneytracker.validation;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import com.starbuck.moneytracker.util.UserDateUtil;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotBeforeCurrentMonthInZoneValidator
        implements ConstraintValidator<NotBeforeCurrentMonthInZone, LocalDate> {

    private final Clock clock;
    private final UserDateUtil userDateUtil;

    /** null esetén a user időzónája */
    private ZoneId zone;

    public NotBeforeCurrentMonthInZoneValidator(Clock clock, UserDateUtil userDateUtil) {
        this.clock = clock;
        this.userDateUtil = userDateUtil;
    }

    @Override
    public void initialize(NotBeforeCurrentMonthInZone annotation) {
        zone = annotation.zone().isBlank() ? null : ZoneId.of(annotation.zone());
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        LocalDate today = zone == null ? userDateUtil.today() : LocalDate.now(clock.withZone(zone));
        LocalDate firstDayOfCurrentMonth = today.withDayOfMonth(1);
        return !value.isBefore(firstDayOfCurrentMonth);
    }
}
