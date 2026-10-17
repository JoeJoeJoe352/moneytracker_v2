package com.starbuck.moneytracker.unit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.starbuck.moneytracker.config.TimeZoneAwareLocaleResolver;

public class TimeZoneAwareLocaleResolverTest {

    private TimeZoneAwareLocaleResolver resolver;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        resolver = new TimeZoneAwareLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        request = new MockHttpServletRequest();
    }

    private TimeZone resolveTimeZone() {
        LocaleContext context = resolver.resolveLocaleContext(request);
        return assertInstanceOf(TimeZoneAwareLocaleContext.class, context).getTimeZone();
    }

    @Test
    void testResolvesTimeZoneFromHeader() {
        request.addHeader(TimeZoneAwareLocaleResolver.TIME_ZONE_HEADER, "Europe/Budapest");

        assertEquals(TimeZone.getTimeZone("Europe/Budapest"), resolveTimeZone());
    }

    @Test
    void testMissingHeaderFallsBackToUtc() {
        assertEquals(TimeZoneAwareLocaleResolver.DEFAULT_TIME_ZONE, resolveTimeZone());
    }

    @Test
    void testBlankHeaderFallsBackToUtc() {
        request.addHeader(TimeZoneAwareLocaleResolver.TIME_ZONE_HEADER, "  ");

        assertEquals(TimeZoneAwareLocaleResolver.DEFAULT_TIME_ZONE, resolveTimeZone());
    }

    @Test
    void testInvalidHeaderFallsBackToUtc() {
        request.addHeader(TimeZoneAwareLocaleResolver.TIME_ZONE_HEADER, "Mars/Olympus_Mons");

        assertEquals(TimeZoneAwareLocaleResolver.DEFAULT_TIME_ZONE, resolveTimeZone());
    }

    @Test
    void testLocaleIsStillResolvedFromAcceptLanguage() {
        request.addHeader("Accept-Language", "de");
        request.addHeader(TimeZoneAwareLocaleResolver.TIME_ZONE_HEADER, "Europe/Berlin");

        assertEquals(Locale.GERMAN, resolver.resolveLocaleContext(request).getLocale());
    }

    @Test
    void testSetLocaleContextIsNotSupported() {
        assertThrowsExactly(UnsupportedOperationException.class,
                () -> resolver.setLocaleContext(request, new MockHttpServletResponse(), null));
    }
}
