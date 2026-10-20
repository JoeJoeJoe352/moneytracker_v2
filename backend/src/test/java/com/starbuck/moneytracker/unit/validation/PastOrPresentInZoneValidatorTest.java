package com.starbuck.moneytracker.unit.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.starbuck.moneytracker.validation.PastOrPresentInZone;
import com.starbuck.moneytracker.validation.PastOrPresentInZoneValidator;

public class PastOrPresentInZoneValidatorTest {

    /** UTC szerint 2026-10-01 22:30, ami több zónában már másnap van */
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-01T22:30:00Z"), ZoneOffset.UTC);

    private final PastOrPresentInZoneValidator validator =
            new PastOrPresentInZoneValidator(clock, new UserDateUtil(clock));

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @ParameterizedTest(name = "{0}, {1} -> {2}")
    @CsvSource({
        "Europe/Budapest, 2026-10-02, true", // Budapesten már 10-02 van
        "Europe/Budapest, 2026-10-03, false",
        "UTC, 2026-10-01, true",
        "UTC, 2026-10-02, false",
        "America/New_York, 2026-09-30, true",
        "America/New_York, 2026-10-02, false"
    })
    void testExplicitZone(String zone, LocalDate date, boolean expected) {
        validator.initialize(annotation(zone));

        assertEquals(expected, validator.isValid(date, null));
    }

    @Test
    void testExplicitZoneIgnoresUserTimeZone() {
        LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("Europe/Budapest"));
        validator.initialize(annotation("UTC"));

        assertEquals(false, validator.isValid(LocalDate.of(2026, 10, 2), null));
    }

    @Test
    void testEmptyZoneUsesUserTimeZone() {
        LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("Europe/Budapest"));
        validator.initialize(annotation(""));

        assertTrue(validator.isValid(LocalDate.of(2026, 10, 2), null));
        assertEquals(false, validator.isValid(LocalDate.of(2026, 10, 3), null));
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

    private static PastOrPresentInZone annotation(String zone) {
        PastOrPresentInZone annotation = mock(PastOrPresentInZone.class);
        when(annotation.zone()).thenReturn(zone);
        return annotation;
    }
}
