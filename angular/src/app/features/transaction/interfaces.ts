import { FormControl } from '@angular/forms';
import { DropdownInterface } from '../../shared/interfaces';
import { CurrencyCodesEnum } from '../../shared/enums';

/**
 * Detail form elemei
 */
export interface DetailForm {
    detailName: FormControl<string>;
    detailPrice: FormControl<number | null>;
    detailWeight: FormControl<number | null>;
    detailUnitPrice: FormControl<number | null>;
    detailIsComplexPriceMode: FormControl<boolean>;
    categories: FormControl<DropdownInterface[]>;
}

/**
 * Tranzakciós adatok, ahogy a form inpujai elvárja kezdőértéknek, plusz a detailok
 */
export interface TransactionInputDefaultValuesWithDetails {
    name: string;
    isIncome: boolean;
    price: null | number;
    categories: null | number[];
    transactionDate: Date | null;
    details: TransactionDetailsDataFromBackend[];
    isComplexTransaction: boolean;
    walletId: number;
}

/**
 * Tranzakciós adatok, ahogy a form kitöltése után lesznek
 * itt még nem lehet null a price
 */
export interface NewTransaction {
    name: string;
    isIncome: boolean;
    price: number | null;
    transactionDate: Date;
    categories: DropdownInterface[];
    walletId: number;
    details: {
        detailName: string;
        detailPrice: number;
        detailWeight: number | null;
        detailUnitPrice: number | null;
        categories: DropdownInterface[];
        detailIsComplexPriceMode: boolean;
    }[];
    isComplexTransaction: boolean;
}

/**
 * Tranzakciós adatok, abban a formában, ahogy a backend szeretné feldolgozni
 */
export interface TransactionDataForBackend {
    globalPrice: number | null;
    globalCategories: number[] | null;
    name: string;
    transactionDate: string;
    transactionType: string;
    transactionDetails: TransactionDetailsDataForBackend[];
    walletId: number;
}

/**
 * Detail adatok a backendnek
 */
export interface TransactionDetailsDataForBackend {
    name: string;
    price: number | null;
    weight: number | null;
    unitPrice: number | null;
    categories: number[];
}

/**
 * Tranzakció adatai, abban a formában, ahogyan a backendről jön
 */
export interface TransactionDataFromBackend {
    id: number;
    name: string;
    priceSum: number;
    transactionDate: string;
    transactionType: string;
    isComplexTransaction: boolean;
    transactionDetails: TransactionDetailsDataFromBackend[];
    walletId: number;
}

/**
 * Detailok adatai a backendről
 */
export interface TransactionDetailsDataFromBackend {
    name: string;
    price: number; // ha backendről jön, akkor ez sose lesz null
    weight: number | null;
    unitPrice: number | null;
    isComplexPriceMode: boolean;
    categories: number[];
}

export interface MoneySumInterface {
    incomeSumThisMonth: number;
    expenseSumThisMonth: number;
    moneySum: WalletSummaryInterface[];
}

export interface WalletSummaryInterface {
    currencyCode: CurrencyCodesEnum;
    total: number;
}

export interface CategorySaveRequestInterface {
    name: string;
}

export interface CategoryResponseInterface {
    id: number;
    name: string;
    isDefaultCategory: boolean;
}
