import { Injectable } from '@angular/core';
import {
    NewTransaction,
    TransactionDataForBackend,
    TransactionDataFromBackend,
    TransactionInputDefaultValuesWithDetails,
} from './interfaces';
import { TransactionTypeEnum } from './transaction-type-enum';

@Injectable({
    providedIn: 'root',
})
export class TransactionUtils {
    /**
     * Átalakítja a tranzakciós adat modellt arra formára, amit az inputok elvárnak
     */
    convertDataToInput(
        transaction: TransactionDataFromBackend,
    ): TransactionInputDefaultValuesWithDetails {
        const isIncome = transaction.transactionType === TransactionTypeEnum.INCOME;
        const transactionDate = new Date(transaction.transactionDate);

        return {
            name: transaction.name,
            // Backend negatívba tárolja a kiadást.
            // Ha abszolut értéket vennék, az eltüntethet potenciális hibát
            price: isIncome ? transaction.priceSum : transaction.priceSum * -1,
            isIncome: isIncome,
            transactionDate: transactionDate,
            transactionDetails: transaction.transactionDetails,
        };
    }

    /**
     * A Tranzakciós form inputból kapott adatokat átalakítja a backend számára megfelelő formába
     */
    convertToBackendData(input: NewTransaction): TransactionDataForBackend {
        const { transactionDate, isIncome, price, ...rest } = input;

        const transactionDateString = transactionDate.toISOString().slice(0, 10);

        // átalakítjuk az isIncome mező értékét a backend enum-jára
        const TransactionyTypeString = isIncome
            ? TransactionTypeEnum.INCOME
            : TransactionTypeEnum.OUTCOME;

        // kavarodások elkerülése végett nem lehet negatív értéket beírni az árba a kiadás kapcsoló alapján itt állítjuk át negatív értékre, hogyha kell
        const priceFormatted = isIncome ? price : price * -1;

        return {
            name: rest.name,
            price: priceFormatted,
            transactionType: TransactionyTypeString,
            transactionDate: transactionDateString,
        };
    }
}
