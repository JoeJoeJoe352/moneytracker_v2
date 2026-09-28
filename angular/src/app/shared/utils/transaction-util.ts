import { TransactionTypeEnum } from '@app/shared/enums';
import { TransactionListElementData } from '@app/features/transaction/interfaces';

/**
 * Csak a megadott típusú tranzakciókat adja vissza
 */
export function filterByType(
    transactions: TransactionListElementData[],
    type: TransactionTypeEnum,
): TransactionListElementData[] {
    return transactions.filter((transaction) => transaction.transactionType === type);
}

/**
 * Az értékek összege, üres tömbnél 0
 */
export function sum(values: number[]): number {
    return values.reduce((total, value) => total + value, 0);
}

/**
 * A legnagyobb érték indexe (egyezésnél az elsőé), üres tömbnél -1.
 * (A Math.max(...values) nagy tömbnél hibát okozhat)
 */
export function indexOfMax(values: number[]): number {
    let maxIndex = -1;
    let maxValue = -Infinity;
    for (const [i, value] of values.entries()) {
        if (maxIndex === -1 || value > maxValue) {
            maxIndex = i;
            maxValue = value;
        }
    }
    return maxIndex;
}
