import { describe, it, expect } from 'vitest';
import { FormControl } from '@angular/forms';
import { notBeforeToday } from './not-before-today-validator';

describe('notBeforeToday (Vitest)', () => {
    it('should return null when the date is today', () => {
        const control = new FormControl(new Date());
        expect(notBeforeToday(control)).toBeNull();
    });

    it('should return null when the date is today at midnight', () => {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const control = new FormControl(today);
        expect(notBeforeToday(control)).toBeNull();
    });

    it('should return null when the date is in the future', () => {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        const control = new FormControl(tomorrow);
        expect(notBeforeToday(control)).toBeNull();
    });

    it('should return an error when the date is yesterday', () => {
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);
        yesterday.setHours(23, 59, 59, 999);
        const control = new FormControl(yesterday);
        expect(notBeforeToday(control)).toEqual({ beforeToday: true });
    });

    it('should return null when the value is null (leaves empty-value handling to required)', () => {
        const control = new FormControl(null);
        expect(notBeforeToday(control)).toBeNull();
    });

    it('should return null when the date is invalid (leaves it to validDate)', () => {
        const control = new FormControl(new Date('invalid'));
        expect(notBeforeToday(control)).toBeNull();
    });
});
