package com.starbuck.moneytracker.unit.entity.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.starbuck.moneytracker.entity.enums.RecurrenceIntervalEnum;

public class RecurrenceIntervalEnumTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "2026-10-31, 2026-11-07",
        "2026-12-28, 2027-01-04", // évváltás
        "2028-02-25, 2028-03-03" // szökőév, átlépi a február 29-et
    })
    void testWeeklyAddsOneWeekToActualDate(LocalDate actualDate, LocalDate expected) {
        assertEquals(expected, RecurrenceIntervalEnum.WEEKLY.calculateNextOccurrence(actualDate, actualDate));
    }

    @Test
    void testWeeklyIgnoresStartDate() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate actualDate = LocalDate.of(2026, 10, 31);

        assertEquals(
                LocalDate.of(2026, 11, 7),
                RecurrenceIntervalEnum.WEEKLY.calculateNextOccurrence(startDate, actualDate));
    }

    @ParameterizedTest(name = "start: {0}, actual: {1} -> {2}")
    @CsvSource({
        "2026-10-15, 2026-10-15, 2026-11-15", // hétköznapi eset
        "2026-12-31, 2026-12-31, 2027-01-31", // évváltás
        "2026-01-31, 2026-01-31, 2026-02-28", // rövidebb hónap: utolsó napra áll
        "2028-01-31, 2028-01-31, 2028-02-29", // szökőév
        "2026-01-30, 2026-01-30, 2026-02-28",
        "2026-01-31, 2026-03-31, 2026-04-30" // 30 napos hónap
    })
    void testMonthlyKeepsStartDayOrFallsBackToEndOfMonth(
            LocalDate startDate, LocalDate actualDate, LocalDate expected) {
        assertEquals(expected, RecurrenceIntervalEnum.MONTHLY.calculateNextOccurrence(startDate, actualDate));
    }

    @ParameterizedTest(name = "start: {0}, actual: {1} -> {2}")
    @CsvSource({
        "2026-01-31, 2026-02-28, 2026-03-31",
        "2026-01-30, 2026-02-28, 2026-03-30",
        "2026-01-31, 2026-04-30, 2026-05-31"
    })
    void testMonthlyReturnsToStartDayAfterShorterMonth(LocalDate startDate, LocalDate actualDate, LocalDate expected) {
        // a rövidebb hónap utáni hónapban újra a kezdő nap kell, nem az előző esedékesség napja
        assertEquals(expected, RecurrenceIntervalEnum.MONTHLY.calculateNextOccurrence(startDate, actualDate));
    }
}
