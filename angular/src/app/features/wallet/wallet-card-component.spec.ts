import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { WalletCardComponent } from './wallet-card-component';
import { WalletDataInterface } from './interfaces';
import { CurrencyCodesEnum, WalletTypesEnum } from '../../shared/enums';

describe('WalletCardComponent (Vitest)', () => {
    let fixture: ComponentFixture<WalletCardComponent>;
    let component: WalletCardComponent;

    const wallet: WalletDataInterface = {
        id: 5,
        name: 'Napi költés',
        currencyCode: CurrencyCodesEnum.huf,
        type: WalletTypesEnum.default,
        sum: 550,
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [WalletCardComponent],
            providers: [provideTranslateService()],
        }).compileComponents();

        fixture = TestBed.createComponent(WalletCardComponent);
        component = fixture.componentInstance;
        component.walletData = wallet;
    });

    it('should render the wallet name and currency symbol', () => {
        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('.wallet-name').textContent.trim()).toBe(
            'Napi költés',
        );
        expect(fixture.nativeElement.querySelector('.wallet-balance-amount').textContent.trim()).toBe(
            '550',
        );
        expect(
            fixture.nativeElement.querySelector('.wallet-balance-currency').textContent.trim(),
        ).toBe('Ft');
    });

    it('should render the euro symbol for an EUR wallet', () => {
        component.walletData = { ...wallet, currencyCode: CurrencyCodesEnum.eur };
        fixture.detectChanges();

        expect(
            fixture.nativeElement.querySelector('.wallet-balance-currency').textContent.trim(),
        ).toBe('€');
    });

    it('should emit cardClicked with the wallet id when clicked', () => {
        fixture.detectChanges();

        let clickedId: number | undefined;
        component.cardClicked.subscribe((id) => (clickedId = id));

        fixture.nativeElement.querySelector('.wallet-card').click();

        expect(clickedId).toBe(5);
    });

    it('should emit cardClicked with the wallet id on enter keydown', () => {
        fixture.detectChanges();

        let clickedId: number | undefined;
        component.cardClicked.subscribe((id) => (clickedId = id));

        const card = fixture.nativeElement.querySelector('.wallet-card');
        card.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

        expect(clickedId).toBe(5);
    });
});
