import { Injectable } from '@angular/core';
import {
    NewTransaction,
    TransactionDataForBackend,
    TransactionDataFromBackend,
    TransactionDetailsDataForBackend2,
    TransactionDetailsDataFromBackend,
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

        const transactionDetailsFormatted = [] as TransactionDetailsDataFromBackend[];
        transaction.transactionDetails.forEach((detail) => {
            const { price, ...rest } = detail;
            transactionDetailsFormatted.push({
                ...rest,
                // Frontenden a kavarodások elkerülése végett az inputban mindig csak pozitív értékeket fogunk tenni
                price: isIncome ? price : price * -1,
            });
        });

        return {
            name: transaction.name,
            // Backend negatívba tárolja a kiadást.
            // Ha abszolut értéket vennék, az eltüntethet potenciális hibát
            price: isIncome ? transaction.priceSum : transaction.priceSum * -1,
            isIncome: isIncome,
            transactionDate: transactionDate,
            details: transactionDetailsFormatted,
        };
    }

    /**
     * A Tranzakciós form inputból kapott adatokat átalakítja a backend számára megfelelő formába
     */
    convertToBackendData(input: NewTransaction): TransactionDataForBackend {
        const transactionDateString = input.transactionDate.toISOString().slice(0, 10);

        // átalakítjuk az isIncome mező értékét a backend enum-jára
        const TransactionyTypeString = input.isIncome
            ? TransactionTypeEnum.INCOME
            : TransactionTypeEnum.OUTCOME;

        const transactionDetailsFormatted = [] as TransactionDetailsDataForBackend2[];
        if (input.details.length > 0) {
            input.details.forEach((detail) => {
                transactionDetailsFormatted.push({
                    name: detail.detailName,
                    // Minden tranzakcióban csak egyfajta előjelű szám (minden vagy pozitív, vagy negatív) lehet. Ezt a TransactionyType mondja meg kapcsoló állása alapján
                    // itt állítjuk át a priceokat negatív értékre, hogyha kell
                    price: input.isIncome ? detail.detailPrice : detail.detailPrice * -1,
                });
            });
        }

        return {
            globalPrice: input.isIncome || input.price === null ? input.price : input.price * -1,
            name: input.name,
            transactionType: TransactionyTypeString,
            transactionDate: transactionDateString,
            transactionDetails: transactionDetailsFormatted,
        };
    }
}
