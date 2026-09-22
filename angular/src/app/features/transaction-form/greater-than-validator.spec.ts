import { describe, it, expect } from 'vitest';
import { FormControl } from '@angular/forms';
import { greaterThan } from './greater-than-validator';

describe('greaterThan (Vitest)', () => {
    it('should return null when the value is strictly greater than the limit', () => {
        const control = new FormControl(1);
        expect(greaterThan(0)(control)).toBeNull();
    });

    it('should return an error when the value equals the limit', () => {
        const control = new FormControl(0);
        expect(greaterThan(0)(control)).toEqual({ greaterThan: { min: 0, actual: 0 } });
    });

    it('should return an error when the value is less than the limit', () => {
        const control = new FormControl(-1);
        expect(greaterThan(0)(control)).toEqual({ greaterThan: { min: 0, actual: -1 } });
    });

    it('should return null when the value is null (leaves empty-value handling to required)', () => {
        const control = new FormControl(null);
        expect(greaterThan(0)(control)).toBeNull();
    });

    it('should return null when the value is an empty string', () => {
        const control = new FormControl('');
        expect(greaterThan(0)(control)).toBeNull();
    });
});
