import { inject, Pipe, PipeTransform } from '@angular/core';
import { formatCurrency, registerLocaleData } from '@angular/common';
import localeEn from '@angular/common/locales/en';
import localeDe from '@angular/common/locales/de';
import localeHu from '@angular/common/locales/hu';
import { TranslateService } from '@ngx-translate/core';
import { CurrencyCodesEnum } from '../enums';
import { WalletDataUtil } from '../../features/wallet/wallet-data-util';
import { getLocaleForLang } from '../utils/language-util';

registerLocaleData(localeEn);
registerLocaleData(localeDe);
registerLocaleData(localeHu);

/**
 * Wrapper a formatCurrency köré
 */
@Pipe({
    name: 'currencyFormat',
    pure: false,
})
export class CurrencyFormatPipe implements PipeTransform {
    private translateService = inject(TranslateService);
    private walletUtils = inject(WalletDataUtil);

    public transform(amount: number, currencyCode: CurrencyCodesEnum): string {
        if (amount == null) {
            return '';
        }

        const lang = this.translateService.currentLang() ?? this.translateService.getFallbackLang();
        const locale = getLocaleForLang(lang);
        // Az Angular locale-adatai csak a locale "saját" pénznemének szimbólumát ismerik, a többihez az ISO kódot adnak vissza
        // (pl. hu locale-ban EUR -> "EUR", nem "€") - ezért a szimbólumot a WalletDataUtil-ból vesszük
        const symbol = this.walletUtils.getCurrencySymbolForCurrencyCode(currencyCode);

        // A formatCurrency a pénznem szimbólum pozícióját (szám elé/mögé) és az ezres elválasztókat a UI nyelvéhez igazítja.
        // Nem hozunk létre CurrencyPipe példányt, mert az impure pipe miatt minden change detection ciklusban újra kéne
        return formatCurrency(amount, locale, symbol, currencyCode, '1.0-2');
    }
}
