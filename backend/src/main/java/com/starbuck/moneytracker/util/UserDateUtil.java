package com.starbuck.moneytracker.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.config.TimeZoneAwareLocaleResolver;

@Component
public class UserDateUtil {

    private final Clock clock;

    public UserDateUtil(Clock clock) {
        this.clock = clock;
    }

    /**
     * A user időzónája szerinti mai nap
     *
     * @return LocalDate
     */
    public LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), getUserZone());
    }

    /**
     * A requesthez tartozó user időzónája (a frontend x-time-zone headeréből, TimeZoneAwareLocaleResolver).
     * Request nélkül (ütemezett feladat, async szál) UTC - a LocaleContextHolder.getTimeZone() ilyenkor
     * a JVM alapértelmezett zónáját adná, ami gépenként eltérhet
     *
     * @return ZoneId
     */
    public ZoneId getUserZone() {
        if (LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext context
                && context.getTimeZone() != null) {
            return context.getTimeZone().toZoneId();
        }
        return TimeZoneAwareLocaleResolver.DEFAULT_TIME_ZONE.toZoneId();
    }
}
