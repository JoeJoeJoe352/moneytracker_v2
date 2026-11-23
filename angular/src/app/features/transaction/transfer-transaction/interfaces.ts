/**
 * Az átvezetés form érvényes értéke
 */
export interface TransferTransactionFormValue {
    srcTransactionName: string;
    srcWalletId: number;
    dstTransactionName: string;
    dstWalletId: number;
    transactionDate: Date;
    /**
     * Az átvezetett összeg (pozitív, az előjelet a backend adja oldalanként)
     */
    priceSum: number;
}

/**
 * Átvezetés létrehozási kérés a backend felé
 */
export interface TransferTransactionCreateRequest {
    srcTransactionName: string;
    srcWalletId: number;
    dstTransactionName: string;
    dstWalletId: number;
    /**
     * yyyy-MM-dd formátumú dátum
     */
    transactionDate: string;
    priceSum: number;
}
