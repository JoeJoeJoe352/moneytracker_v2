/**
 * Tranzakciós adatok, ahogy a form inpujai elvárja kezdőértéknek, plusz a detailok
 */
export interface TransactionInputDefaultValuesWithDetails {
    name: string;
    isIncome: boolean;
    price: null | number;
    transactionDate: Date;
    transactionDetails: TransactionDetailsDataFromBackend[];
}

/**
 * Tranzakciós adatok, ahogy a form kitöltése után lesznek
 * Kezdőértéknek lehet null a price, de elküldés után már nem
 */
export interface NewTransaction extends Omit<TransactionInputDefaultValuesWithDetails, 'price'> {
    price: number;
}

/**
 * Tranzakciós adatok, abban a formában, ahogy a backend feldolgozná
 */
export interface TransactionDataForBackend {
    name: string;
    price: number;
    transactionDate: string;
    transactionType: string;
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
    price: number | null;
    weight: number | null;
    unitPrice: number | null;
}
