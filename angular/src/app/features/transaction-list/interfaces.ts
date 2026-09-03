import { WalletDataInterfaceWithoutSum } from "../wallet/interfaces";

/**
 * Egy, a listában szereplő elem adatai
 */
export interface TransactionListElementData {
    id: number;
    name: string;
    priceSum: number;
    transactionDate: string;
    transactionType: string;
    isComplexTransaction: boolean;
    transactionDetails: TransactionDetailListElementData[];
    wallet: WalletDataInterfaceWithoutSum
}

/**
 * Egy, a listában szereplő tranzakcióhoz kapcsolodó egyik detail adatai
 */
export interface TransactionDetailListElementData {
    name: string;
    price: number;
    weight: number | null;
    unitPrice: number | null;
    isComplexPriceMode: boolean;
    categories: string[];
}