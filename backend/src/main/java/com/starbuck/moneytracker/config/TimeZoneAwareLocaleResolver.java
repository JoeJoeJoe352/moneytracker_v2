package com.starbuck.moneytracker.config;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TimeZone;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext;
import org.springframework.web.servlet.LocaleContextResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A nyelvet az Accept-Language headerből, az időzónát a frontend által küldött headerből olvassa ki.
 */
public class TimeZoneAwareLocaleResolver extends AcceptHeaderLocaleResolver implements LocaleContextResolver {

    /** Angular TimeZoneInterceptor által küldött header, IANA zóna azonosítóval (pl.: Europe/Budapest) */
    public static final String TIME_ZONE_HEADER = "x-time-zone";

    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone(ZoneOffset.UTC);

    @Override
    public LocaleContext resolveLocaleContext(HttpServletRequest request) {
        return new SimpleTimeZoneAwareLocaleContext(resolveLocale(request), resolveTimeZone(request));
    }

    @Override
    public void setLocaleContext(HttpServletRequest request, HttpServletResponse response,
            LocaleContext localeContext) {
        throw new UnsupportedOperationException(
                "Cannot change locale context - use the Accept-Language and " + TIME_ZONE_HEADER + " headers");
    }

    private TimeZone resolveTimeZone(HttpServletRequest request) {
        String header = request.getHeader(TIME_ZONE_HEADER);
        if (header == null || header.isBlank()) {
            // Pl.: postmanből nem jön header
            return DEFAULT_TIME_ZONE;
        }

        try {
            return TimeZone.getTimeZone(ZoneId.of(header.trim()));
        } catch (DateTimeException exception) {
            // ismeretlen / hibás zóna miatt ne haljon el a request
            return DEFAULT_TIME_ZONE;
        }
    }
}
