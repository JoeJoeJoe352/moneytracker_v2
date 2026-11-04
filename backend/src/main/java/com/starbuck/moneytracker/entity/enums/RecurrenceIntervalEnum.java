package com.starbuck.moneytracker.entity.enums;

import java.time.LocalDate;

public enum RecurrenceIntervalEnum {
    WEEKLY {
        @Override
        public LocalDate calculateNextOccurrence(LocalDate startDate, LocalDate actualDate) {
            return actualDate.plusWeeks(1);
        }
    },
    MONTHLY {
        /**
         * start date-ből indulunk ki, mert az tartalmazza a napot, amikor elindította a
         * user a kalkulációt
         * A nextOccurrence-kre nem támaszkodhatunk, mert ha rövidebb a hónap, akkor
         * lehet kiesik az a nap, amikor indította a user
         * Pl.: január 31.-én indul, február 28.-án lesz új tranzakció, viszont
         * márciusban újra 31.-én kell lenni
         */
        @Override
        public LocalDate calculateNextOccurrence(LocalDate startDate, LocalDate actualDate) {
            LocalDate nextOccurrenceRaw = actualDate.plusMonths(1);
            int dayFromStartDate = startDate.getDayOfMonth();
            int nextOccurrenceDay = Math.min(nextOccurrenceRaw.lengthOfMonth(), dayFromStartDate);

            return LocalDate.of(nextOccurrenceRaw.getYear(), nextOccurrenceRaw.getMonth(), nextOccurrenceDay);
        }
    };

    /**
     * Kiszámolja a következő tranzakció létrehozás dátumát
     */
    public abstract LocalDate calculateNextOccurrence(LocalDate startDate, LocalDate actualDate);
}
