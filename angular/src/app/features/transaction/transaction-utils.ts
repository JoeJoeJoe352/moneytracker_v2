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
            categories: transaction.isComplexTransaction
                ? []
                : transaction.transactionDetails[0].categories,
        };
    }

    /**
     * A Tranzakciós form inputból kapott adatokat átalakítja a backend számára megfelelő formába
     */
    convertToBackendData(input: NewTransaction): TransactionDataForBackend {
        // sv-SE => YYYY-MM-DD formátumot fog visszaadni, amit elfogad a Java LocalDate típusa
        const transactionDateString = input.transactionDate.toLocaleDateString('sv-SE');

        // átalakítjuk az isIncome mező értékét a backend enum-jára
        const TransactionTypeString = input.isIncome
            ? TransactionTypeEnum.INCOME
            : TransactionTypeEnum.OUTCOME;

        // Ha egyszerű transaction-ról van szó, akkor nem küldjük el a detail adatokat, mert a globalPrice input érték lesz a mérvadó
        const shouldSendDetails = input.details.length > 0 && input.isComplexTransaction;
        const transactionDetailsFormatted = shouldSendDetails
            ? input.details.map((detail) => ({
                  name: detail.detailName,
                  price: detail.detailIsComplexPriceMode
                      ? null
                      : this.deNormalizePriceForBackend(detail.detailPrice, input.isIncome),
                  weight: detail.detailIsComplexPriceMode ? detail.detailWeight : null,
                  unitPrice: detail.detailIsComplexPriceMode ? detail.detailUnitPrice : null,
                  categories: detail.categories.map((category) => category.item_id),
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
            globalCategories: input.categories.map((category) => category.item_id),
        };
    }

    /**
     * Normalizálja a kapott számot, hogy mindig pozitív legyen
     * Frontenden a kavarodások elkerülése végett a price inputokba mindig csak pozitív értékeket fogunk tenni.
     */
    private normalizePriceForFrontend(denormalizedPrice: number, condition: boolean): number {
        // Azért nem csak sima abszolút érték, mert az elfedhet potenciális hibákat
        return condition ? denormalizedPrice : denormalizedPrice * -1;
    }

    /**
     * Denormalizálja a kapott számot, a condition függvényében pozitív, vagy negatív számmá
     * Szükség esetén visszaalakítjuk a price számot negatívvá, mert backend előjelesen tárolja a kiadást
     */
    private deNormalizePriceForBackend(normalizedPrice: number, condition: boolean): number {
        // Azért nem csak sima abszolút érték, mert az elfedhet potenciális hibákat
        return condition ? normalizedPrice : normalizedPrice * -1;
    }
}
