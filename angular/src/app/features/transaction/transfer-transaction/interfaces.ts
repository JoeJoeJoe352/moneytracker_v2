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
 * Egy meglévő átvezetés adatai a backendtől (szerkesztéshez)
 */
export interface TransferTransactionResponse {
    /**
     * Az átvezetés id-ja (nem a tranzakcióké), ezzel módosítható és törölhető
     */
    transferId: number;
    srcTransactionName: string;
    srcWalletId: number;
    dstTransactionName: string;
    dstWalletId: number;
    /**
     * yyyy-MM-dd formátumú dátum
     */
    transactionDate: string;
    /**
     * Az átvezetett összeg (pozitív)
     */
    priceSum: number;
}

/**
 * Átvezetés módosítási kérés a backend felé. A walletek nem módosíthatók
 */
export interface TransferTransactionUpdateRequest {
    srcTransactionName: string;
    dstTransactionName: string;
    /**
     * yyyy-MM-dd formátumú dátum
     */
    transactionDate: string;
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
