import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { CurrencyCodesEnum } from '@app/shared/enums';
import { CurrencyConversionService } from './currency-conversion-service';

describe('CurrencyConversionService (Vitest)', () => {
    let service: CurrencyConversionService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(CurrencyConversionService);
    });

    it('should return the amount unchanged for HUF', () => {
        expect(service.toHuf(1000, CurrencyCodesEnum.huf)).toBe(1000);
    });

    it('should convert EUR to HUF', () => {
        expect(service.toHuf(10, CurrencyCodesEnum.eur)).toBe(3650);
    });

    it('should convert USD to HUF', () => {
        expect(service.toHuf(10, CurrencyCodesEnum.usd)).toBe(3200);
    });

    it('should keep the sign of negative amounts', () => {
        expect(service.toHuf(-10, CurrencyCodesEnum.eur)).toBe(-3650);
    });
});
