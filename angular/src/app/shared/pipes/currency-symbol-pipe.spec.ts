import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { CurrencySymbolPipe } from './currency-symbol-pipe';
import { CurrencyCodesEnum } from '../enums';

describe('CurrencySymbolPipe (Vitest)', () => {
    let pipe: CurrencySymbolPipe;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        pipe = TestBed.runInInjectionContext(() => new CurrencySymbolPipe());
    });

    it.each([
        [CurrencyCodesEnum.huf, 'Ft'],
        [CurrencyCodesEnum.eur, '€'],
        [CurrencyCodesEnum.usd, '$'],
    ])('should transform %s into %s, delegating to WalletDataUtil', (currencyCode, expected) => {
        expect(pipe.transform(currencyCode)).toBe(expected);
    });
});
