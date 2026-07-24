/**
 * Tranzakciós adatok, ahogy a form inpujai elvárja kezdőértéknek, plusz a detailok
 */
export interface TransactionInputDefaultValuesWithDetails {
    name: string;
    isIncome: boolean;
    price: null | number;
    transactionDate: Date;
    details: TransactionDetailsDataFromBackend[];
}

/**
 * Tranzakciós adatok, ahogy a form kitöltése után lesznek
 * itt már nem lehet null a price, de elküldés után már nem
 */
export interface NewTransaction {
    name: string;
    isIncome: boolean;
    price: number|null;
    transactionDate: Date;
    details: { detailName: string; detailPrice: number }[];
}

/**
 * Tranzakciós adatok, abban a formában, ahogy a backend szeretné feldolgozni
 */
export interface TransactionDataForBackend {
    globalPrice: number|null;
    name: string;
    transactionDate: string;
    transactionType: string;
    transactionDetails: TransactionDetailsDataForBackend2[];
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
    price: number; // todo ez később lehet null
    weight: number | null;
    unitPrice: number | null;
}

// todo ez majd később nem kell, TransactionDetailsDataFromBackend-el helyettesíthető
export interface TransactionDetailsDataForBackend2 {
    name: string;
    price: number;
}
