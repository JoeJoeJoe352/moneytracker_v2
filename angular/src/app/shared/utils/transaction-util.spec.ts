import { describe, it, expect } from 'vitest';
import { indexOfMax } from './transaction-util';

describe('transaction-util (Vitest)', () => {
    it('should return the index of the largest value', () => {
        expect(indexOfMax([3, 9, 2])).toBe(1);
    });

    it('should return the first index when the largest value appears more than once', () => {
        expect(indexOfMax([5, 9, 9, 1])).toBe(1);
    });

    it('should return -1 for an empty array', () => {
        expect(indexOfMax([])).toBe(-1);
    });
});
