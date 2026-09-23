import { Injectable } from '@angular/core';
import { CurrencyCodesEnum, WalletTypesEnum } from '@shared/enums';
import { WalletSummaryInterface } from '../transaction/interfaces';

const WALLET_TYPE_KEYS: Record<WalletTypesEnum, string> = {
    [WalletTypesEnum.default]: 'wallet.type.default',
    [WalletTypesEnum.savings]: 'wallet.type.savings',
};

@Injectable({
    providedIn: 'root',
})
export class WalletDataUtil {
    /**
     * Visszaadja a wallet típusát az enum értéke alapján
     */
    public getLangForWalletType = (type: WalletTypesEnum) => WALLET_TYPE_KEYS[type] ?? '';

    /**
     * Visszaadja a pénznem szöveges reprezentációját az enum értéke alapján
     */
    public getCurrencyTextForCurrencyCode(currencyCode: CurrencyCodesEnum): string {
        switch (currencyCode) {
            case CurrencyCodesEnum.huf:
                return 'currency.huf';
            case CurrencyCodesEnum.eur:
                return 'currency.eur';
            case CurrencyCodesEnum.usd:
                return 'currency.usd';
            default:
                console.error('Unknown currency code const: ' + currencyCode);
                return '';
        }
    }

    /**
     * Visszaadja a pénznem szimbólumát az enum értéke alapján
     */
    public getCurrencySymbolForCurrencyCode(currencyCode: CurrencyCodesEnum): string {
        switch (currencyCode) {
            case CurrencyCodesEnum.huf:
                return 'Ft';
            case CurrencyCodesEnum.eur:
                return '€';
            case CurrencyCodesEnum.usd:
                return '$';
            default:
                console.error('Unknown currency code const: ' + currencyCode);
                return '';
        }
    }

    /**
     * Összegzi valutánként a walleteken lévő pénzt, hogy minden pénznem csak egyszer szerepeljen
     */
    public summarizeSumPerCurrency(
        walletSummary: WalletSummaryInterface[],
    ): WalletSummaryInterface[] {
        const totals = new Map<CurrencyCodesEnum, number>();
        for (const { currencyCode, total } of walletSummary) {
            totals.set(currencyCode, (totals.get(currencyCode) ?? 0) + total);
        }
        return Array.from(totals, ([currencyCode, total]) => ({ currencyCode, total }));
    }
}
