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

        const transactionDetailsFormatted = transaction.transactionDetails.map((detail) => ({
            ...detail,
            price: this.normalizePriceForFrontend(detail.price, isIncome),
        }));

        return {
            name: transaction.name,
            price: this.normalizePriceForFrontend(transaction.priceSum, isIncome),
            isIncome: isIncome,
            transactionDate: transactionDate,
            isComplexTransaction: transaction.isComplexTransaction,
            details: transactionDetailsFormatted,
        };
    }

    /**
     * A Tranzakciós form inputból kapott adatokat átalakítja a backend számára megfelelő formába
     */
    convertToBackendData(input: NewTransaction): TransactionDataForBackend {
        const transactionDateString = input.transactionDate.toISOString().split('T')[0];

        // átalakítjuk az isIncome mező értékét a backend enum-jára
        const TransactionTypeString = input.isIncome
            ? TransactionTypeEnum.INCOME
            : TransactionTypeEnum.OUTCOME;

        // Ha egyszerű transaction-ról van szó, akkor nem küldjük el a detail adatokat, mert a globalPrice input érték lesz a mérvadó
        const shouldSendDetails = input.details.length > 0 && input.isComplexTransaction;
        const transactionDetailsFormatted = shouldSendDetails
            ? input.details.map((detail) => ({
                  name: detail.detailName,
                  price: this.deNormalizePriceForBackend(detail.detailPrice, input.isIncome),
                  weight: detail.detailWeight,
                  unitPrice: detail.detailUnitPrice,
              }))
            : [];

        return {
            globalPrice:
                input.price !== null
                    ? this.deNormalizePriceForBackend(input.price, input.isIncome)
                    : null,
            name: input.name,
            transactionType: TransactionTypeString,
            transactionDate: transactionDateString,
            transactionDetails: transactionDetailsFormatted,
        };
    }

    /**
     * Normalizálja a kapott számot, hogy mindig pozitív legyen
     * Frontenden a kavarodások elkerülése végett a price inputokba mindig csak pozitív értékeket fogunk tenni.
     */
    private normalizePriceForFrontend(denormalizedPrice: number, condition: boolean): number {
        // Azért nem csak sima abszolút érték, mert az eltüntethet potenciális hibát
        return condition ? denormalizedPrice : denormalizedPrice * -1;
    }

    /**
     * Denormalizálja a kapott számot, a condition függvényében pozitív, vagy negatív számmá
     * Szükség esetén visszaalakítjuk a price számot negatívvá, mert backend előjelesen tárolja a kiadást
     */
    private deNormalizePriceForBackend(normalizedPrice: number, condition: boolean): number {
        // Azért nem csak sima abszolút érték, mert az eltüntethet potenciális hibát
        return condition ? normalizedPrice : normalizedPrice * -1;
    }
}
