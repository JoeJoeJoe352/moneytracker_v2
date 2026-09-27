import { describe, it, expect } from 'vitest';
import { CurrencyCodesEnum, TransactionTypeEnum, WalletTypesEnum } from '@app/shared/enums';
import { TransactionListElementData } from '@app/features/transaction/interfaces';
import { filterByType, indexOfMax, sum } from './transaction-util';

function createTransaction(
    id: number,
    transactionType: TransactionTypeEnum,
): TransactionListElementData {
    return {
        id,
        name: `transaction ${id}`,
        priceSum: 100,
        transactionDate: '2026-01-01',
        transactionType,
        isComplexTransaction: false,
        transactionDetails: [],
        wallet: {
            id: 1,
            name: 'wallet',
            currencyCode: CurrencyCodesEnum.huf,
            type: WalletTypesEnum.default,
        },
    };
}

describe('transaction-util (Vitest)', () => {
    const transactions = [
        createTransaction(1, TransactionTypeEnum.OUTCOME),
        createTransaction(2, TransactionTypeEnum.INCOME),
        createTransaction(3, TransactionTypeEnum.OUTCOME),
    ];

    it('should keep only the outcome transactions, in their original order', () => {
        const result = filterByType(transactions, TransactionTypeEnum.OUTCOME);

        expect(result.map((transaction) => transaction.id)).toEqual([1, 3]);
    });

    it('should keep only the income transactions', () => {
        const result = filterByType(transactions, TransactionTypeEnum.INCOME);

        expect(result.map((transaction) => transaction.id)).toEqual([2]);
    });

    it('should return a new array and leave the input untouched', () => {
        const result = filterByType(transactions, TransactionTypeEnum.OUTCOME);
        result.reverse();

        expect(transactions.map((transaction) => transaction.id)).toEqual([1, 2, 3]);
    });

    it('should return an empty array when filtering an empty list', () => {
        expect(filterByType([], TransactionTypeEnum.INCOME)).toEqual([]);
    });

    it('should add up the values', () => {
        expect(sum([100, 250.5, 49.5])).toBe(400);
    });

    it('should return 0 as the sum of an empty array', () => {
        expect(sum([])).toBe(0);
    });

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
