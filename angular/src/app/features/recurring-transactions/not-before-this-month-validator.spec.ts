import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { FormControl } from '@angular/forms';
import { notBeforeThisMonth } from './not-before-this-month-validator';

/**
 * A "mai nap" rögzítve van, így a hónaphatárok bármelyik napon futtatva ugyanúgy tesztelhetők
 */
describe('notBeforeThisMonth (Vitest)', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        // hónap közepe, hogy a hónapon belüli, de a mai nap előtti napok is tesztelhetők legyenek
        vi.setSystemTime(new Date(2026, 10, 15, 10, 30));
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    function validate(value: Date | null) {
        return notBeforeThisMonth(new FormControl(value));
    }

    it('should return null when the date is today', () => {
        expect(validate(new Date(2026, 10, 15))).toBeNull();
    });

    it('should return null when the date is earlier this month (before today)', () => {
        expect(validate(new Date(2026, 10, 3))).toBeNull();
    });

    it('should return null when the date is the first day of the current month', () => {
        expect(validate(new Date(2026, 10, 1))).toBeNull();
    });

    it('should return null when the date is the first day of the current month at midnight', () => {
        expect(validate(new Date(2026, 10, 1, 0, 0, 0, 0))).toBeNull();
    });

    it('should return null when the date is in a later month', () => {
        expect(validate(new Date(2026, 11, 1))).toBeNull();
    });

    it('should return an error when the date is the last day of the previous month', () => {
        expect(validate(new Date(2026, 9, 31, 23, 59, 59, 999))).toEqual({ beforeThisMonth: true });
    });

    it('should return an error when the date is the same month of the previous year', () => {
        expect(validate(new Date(2025, 10, 15))).toEqual({ beforeThisMonth: true });
    });

    it('should use the current month on its first day too', () => {
        vi.setSystemTime(new Date(2026, 10, 1, 0, 30));

        expect(validate(new Date(2026, 10, 1))).toBeNull();
        expect(validate(new Date(2026, 9, 31))).toEqual({ beforeThisMonth: true });
    });

    it('should handle the year boundary (January vs. December of the previous year)', () => {
        vi.setSystemTime(new Date(2027, 0, 10));

        expect(validate(new Date(2027, 0, 1))).toBeNull();
        expect(validate(new Date(2026, 11, 31))).toEqual({ beforeThisMonth: true });
    });

    it('should return null when the value is null (leaves empty-value handling to required)', () => {
        expect(validate(null)).toBeNull();
    });

    it('should return null when the date is invalid (leaves it to validDate)', () => {
        expect(validate(new Date('invalid'))).toBeNull();
    });
});
