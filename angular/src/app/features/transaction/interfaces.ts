/**
 * Tranzakciós adatok, ahogy a form inpujai elvárja kezdőértéknek, plusz a detailok
 */
export interface TransactionInputDefaultValuesWithDetails {
    name: string;
    isIncome: boolean;
    price: null | number;
    transactionDate: Date | null;
    details: TransactionDetailsDataFromBackend[];
    isComplexTransaction: boolean;
}

/**
 * Tranzakciós adatok, ahogy a form kitöltése után lesznek
 * itt már nem lehet null a price, de elküldés után már nem
 */
export interface NewTransaction {
    name: string;
    isIncome: boolean;
    price: number | null;
    transactionDate: Date;
    details: {
        detailName: string;
        detailPrice: number;
        detailWeight: number;
        detailUnitPrice: number;
    }[];
    isComplexTransaction: boolean;
}

/**
 * Tranzakciós adatok, abban a formában, ahogy a backend szeretné feldolgozni
 */
export interface TransactionDataForBackend {
    globalPrice: number | null;
    name: string;
    transactionDate: string;
    transactionType: string;
    transactionDetails: TransactionDetailsDataForBackend[];
}

/**
 * Detail adatok a backendnek
 */
export interface TransactionDetailsDataForBackend {
    name: string;
    price: number | null;
    weight: number | null;
    unitPrice: number | null;
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
}

export interface MoneySumInterface {
    incomeSumThisMonth: number;
    expenseSumThisMonth: number;
    moneySum: number;
}