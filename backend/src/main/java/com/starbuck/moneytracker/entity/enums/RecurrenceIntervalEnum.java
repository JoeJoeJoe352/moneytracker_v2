package com.starbuck.moneytracker.entity.enums;

import java.time.LocalDate;

public enum RecurrenceIntervalEnum {
    WEEKLY {
        @Override
        public LocalDate calculateNextOccurence(LocalDate startDate) {
            return startDate.plusWeeks(1);
        }
    },
    MONTHLY {
        @Override
        public LocalDate calculateNextOccurence(LocalDate startDate) {
            return startDate.plusMonths(1);
        }
    };

    /**
     * Kiszámolja a következő tranzakció létrehozás dátumát
     */
    public abstract LocalDate calculateNextOccurence(LocalDate startDate);
}
