import { Injectable } from '@angular/core';
import { CurrencyCodesEnum } from '@app/shared/enums';

// TODO ezeket majd éles adatokkal számolni
const CONVERTING_VALUE_FROM_EUR = 365;
const CONVERTING_VALUE_FROM_USD = 320;

@Injectable({
    providedIn: 'root',
})
export class CurrencyConversionService {
    /**
     * Az összeget átváltja forintra a megadott valutából
     */
    toHuf(amount: number, currencyCode: CurrencyCodesEnum): number {
        switch (currencyCode) {
            case CurrencyCodesEnum.huf:
                return amount;
            case CurrencyCodesEnum.eur:
                return amount * CONVERTING_VALUE_FROM_EUR;
            case CurrencyCodesEnum.usd:
                return amount * CONVERTING_VALUE_FROM_USD;
        }
    }
}
