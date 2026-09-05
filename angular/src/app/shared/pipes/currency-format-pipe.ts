import { inject, Pipe, PipeTransform } from '@angular/core';
import { CurrencyPipe, registerLocaleData } from '@angular/common';
import localeEn from '@angular/common/locales/en';
import localeDe from '@angular/common/locales/de';
import localeHu from '@angular/common/locales/hu';
import { TranslateService } from '@ngx-translate/core';
import { CurrencyCodesEnum, SupportedLangEnum } from '../enums';
import { WalletDataUtil } from '../../features/wallet/wallet-data-util';
import { LANGUAGE_TO_LOCALE } from '../utils/language-util';

registerLocaleData(localeEn);
registerLocaleData(localeDe);
registerLocaleData(localeHu);

/**
 * Wrapper a CurrencyPipe köré
 */
@Pipe({
    name: 'currencyFormat',
    standalone: true,
    pure: false,
})
export class CurrencyFormatPipe implements PipeTransform {
    private translateService = inject(TranslateService);
    private walletUtils = inject(WalletDataUtil);

    public transform(amount: number, currencyCode: CurrencyCodesEnum): string {
        // A CLDR pénznem-adatai a szimbólum pozícióját (elé/mögé) és az ezres elválasztókat a UI nyelvéhez (nem a pénznemhez) igazítják
        const lang = this.translateService.currentLang() ?? this.translateService.getFallbackLang();
        const locale = LANGUAGE_TO_LOCALE[lang ?? ''] ?? LANGUAGE_TO_LOCALE[SupportedLangEnum.en];
        // Az Angular locale-adatai csak a locale "saját" pénznemének szimbólumát ismerik (pl. hu -> Ft), a többihez az ISO kódot adnak vissza
        // (pl. hu locale-ban EUR -> "EUR", nem "€") - ezért a szimbólumot a WalletDataUtil-ból vesszük
        const symbol = this.walletUtils.getCurrencySymbolForCurrencyCode(currencyCode);

        return new CurrencyPipe(locale).transform(amount, currencyCode, symbol, '1.0-3') ?? '';
    }
}
