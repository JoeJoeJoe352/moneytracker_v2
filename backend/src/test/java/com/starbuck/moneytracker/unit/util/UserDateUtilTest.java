package com.starbuck.moneytracker.unit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.i18n.LocaleContextHolder;

import com.starbuck.moneytracker.util.UserDateUtil;

public class UserDateUtilTest {

    /** UTC szerint 2026-10-01 22:30, ami több zónában már másnap van */
    private final UserDateUtil userDateUtil = new UserDateUtil(
            Clock.fixed(Instant.parse("2026-10-01T22:30:00Z"), ZoneOffset.UTC));

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "Pacific/Kiritimati, 2026-10-02", // UTC+14
            "Europe/Budapest, 2026-10-02", // UTC+2 (nyári idő)
            "UTC, 2026-10-01",
            "America/New_York, 2026-10-01", // UTC-4
            "Pacific/Pago_Pago, 2026-10-01" // UTC-11
    })
    void testTodayUsesUserTimeZone(String zone, LocalDate expected) {
        LocaleContextHolder.setTimeZone(TimeZone.getTimeZone(zone));

        assertEquals(expected, userDateUtil.today());
    }

    @Test
    void testToUserDateConvertsInstantToUserTimeZone() {
        LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("Europe/Budapest"));

        // UTC szerint még előző nap, Budapesten (UTC+2) már 2026-10-02
        assertEquals(LocalDate.of(2026, 10, 2), userDateUtil.toUserDate(Instant.parse("2026-10-01T23:30:00Z")));
    }

    @Test
    void testTodayWithoutLocaleContextFallsBackToUtc() {
        // pl.: ütemezett feladat / async szál, ahol nincs request
        LocaleContextHolder.resetLocaleContext();

        assertEquals(LocalDate.of(2026, 10, 1), userDateUtil.today());
        assertEquals(ZoneOffset.UTC, userDateUtil.getUserZone().normalized());
    }

    @Test
    void testTodayWithLocaleOnlyContextFallsBackToUtc() {
        // A RequestContextFilter időzóna nélküli contextet állít be, a JVM zónája nem számíthat
        LocaleContextHolder.setLocale(Locale.forLanguageTag("hu"));

        assertEquals(ZoneOffset.UTC, userDateUtil.getUserZone().normalized());
    }
}
