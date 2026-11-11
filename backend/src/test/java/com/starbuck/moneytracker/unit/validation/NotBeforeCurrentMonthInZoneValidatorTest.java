package com.starbuck.moneytracker.unit.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.i18n.LocaleContextHolder;

import com.starbuck.moneytracker.util.UserDateUtil;
import com.starbuck.moneytracker.validation.NotBeforeCurrentMonthInZone;
import com.starbuck.moneytracker.validation.NotBeforeCurrentMonthInZoneValidator;

public class NotBeforeCurrentMonthInZoneValidatorTest {

    /** UTC szerint 2026-10-31 23:30, ami több zónában már november van */
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-31T23:30:00Z"), ZoneOffset.UTC);

    private final NotBeforeCurrentMonthInZoneValidator validator =
            new NotBeforeCurrentMonthInZoneValidator(clock, new UserDateUtil(clock));

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @ParameterizedTest(name = "{0}, {1} -> {2}")
    @CsvSource({
        // UTC szerint még október van: az október eleje és a mai nap előtti októberi napok is érvényesek
        "UTC, 2026-10-01, true",
        "UTC, 2026-10-15, true",
        "UTC, 2026-10-31, true",
        "UTC, 2026-11-20, true",
        "UTC, 2026-09-30, false",
        "UTC, 2025-10-15, false",
        // Budapesten már november van, így az október már nem választható
        "Europe/Budapest, 2026-11-01, true",
        "Europe/Budapest, 2026-10-31, false",
        // New Yorkban még október van
        "America/New_York, 2026-10-01, true",
        "America/New_York, 2026-09-30, false"
    })
    void testExplicitZone(String zone, LocalDate date, boolean expected) {
        validator.initialize(annotation(zone));

        assertEquals(expected, validator.isValid(date, null));
    }

    @Test
    void testExplicitZoneIgnoresUserTimeZone() {
        LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("Europe/Budapest"));
        validator.initialize(annotation("UTC"));

        assertTrue(validator.isValid(LocalDate.of(2026, 10, 1), null));
    }

    @Test
    void testEmptyZoneUsesUserTimeZone() {
        LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("Europe/Budapest"));
        validator.initialize(annotation(""));

        assertTrue(validator.isValid(LocalDate.of(2026, 11, 1), null));
        assertFalse(validator.isValid(LocalDate.of(2026, 10, 31), null));
    }

    @Test
    void testNullIsValid() {
        validator.initialize(annotation("UTC"));

        assertTrue(validator.isValid(null, null));
    }

    @Test
    void testInvalidZoneFailsOnInitialize() {
        assertThrows(DateTimeException.class, () -> validator.initialize(annotation("Not/AZone")));
    }

    private static NotBeforeCurrentMonthInZone annotation(String zone) {
        NotBeforeCurrentMonthInZone annotation = mock(NotBeforeCurrentMonthInZone.class);
        when(annotation.zone()).thenReturn(zone);
        return annotation;
    }
}
