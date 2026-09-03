import { Injectable } from '@angular/core';
import { CurrencyCodesEnum, WalletTypesEnum } from '../../shared/enums';
import { WalletSummaryInterface } from '../transaction/interfaces';

@Injectable({
    providedIn: 'root',
})
export class WalletDataUtil {
    /**
     * Visszaadja a wallet típusát az enum értéke alapján
     */
    public getLangForWalletType(walletType: WalletTypesEnum): string {
        switch (walletType) {
            case WalletTypesEnum.default:
                return 'wallet.type.default';
            case WalletTypesEnum.savings:
                return 'wallet.type.savings';
            default:
                console.error('Unknown wallet type const: ' + walletType);
                return '';
        }
    }

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
        const resultArray: WalletSummaryInterface[] = [];

        walletSummary.forEach((walletData) => {
            const existingItem = resultArray.find(
                (item) => item.currencyCode === walletData.currencyCode,
            );
            
            if (existingItem) {
                existingItem.total += walletData.total;
            } else {
                resultArray.push({ ...walletData });
            }
        });

        return resultArray;
    }
}
