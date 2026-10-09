import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { WalletCardComponent } from './wallet-card-component';
import { WalletDataInterface } from './interfaces';
import { CurrencyCodesEnum, WalletTypesEnum } from '@shared/enums';

describe('WalletCardComponent (Vitest)', () => {
    let fixture: ComponentFixture<WalletCardComponent>;
    let component: WalletCardComponent;

    const wallet: WalletDataInterface = {
        id: 5,
        name: 'Napi költés',
        currencyCode: CurrencyCodesEnum.huf,
        type: WalletTypesEnum.default,
        sum: 550,
        lastSyncDate: '2026-09-15',
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [WalletCardComponent],
            providers: [provideTranslateService()],
        }).compileComponents();

        // hu locale-ban a pénznem szimbóluma az összeg mögé kerül (pl. "550 Ft")
        TestBed.inject(TranslateService).use('hu');

        fixture = TestBed.createComponent(WalletCardComponent);
        component = fixture.componentInstance;
        fixture.componentRef.setInput('walletData', wallet);
    });

    it('should render the wallet name and currency amount with the currency symbol as a suffix', () => {
        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('.wallet-name').textContent.replace(/\s+/g, ' ').trim()).toBe(
            'Napi költés',
        );
        expect(fixture.nativeElement.querySelector('.wallet-balance-amount').textContent.replace(/\s+/g, ' ').trim()).toBe(
            '550 Ft',
        );
    });

    it('should render the euro symbol for an EUR wallet', () => {
        fixture.componentRef.setInput('walletData', { ...wallet, currencyCode: CurrencyCodesEnum.eur });
        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('.wallet-balance-amount').textContent.replace(/\s+/g, ' ').trim()).toBe(
            '550 €',
        );
    });

    it('should emit cardClicked when clicked', () => {
        fixture.detectChanges();

        let clickCount = 0;
        component.cardClicked.subscribe(() => clickCount++);

        fixture.nativeElement.querySelector('.wallet-card').click();

        expect(clickCount).toBe(1);
    });

    it('should emit cardClicked on enter keydown', () => {
        fixture.detectChanges();

        let clickCount = 0;
        component.cardClicked.subscribe(() => clickCount++);

        const card = fixture.nativeElement.querySelector('.wallet-card');
        card.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

        expect(clickCount).toBe(1);
    });

    it('should not emit cardClicked when disabled', () => {
        fixture.componentRef.setInput('disabled', true);
        fixture.detectChanges();

        let clickCount = 0;
        component.cardClicked.subscribe(() => clickCount++);

        const card = fixture.nativeElement.querySelector('.wallet-card');
        card.click();
        card.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

        expect(clickCount).toBe(0);
        expect(card.getAttribute('aria-disabled')).toBe('true');
    });

    it('should render the last sync date', () => {
        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('.sync-date-row').textContent).toContain(
            '2026-09-15',
        );
    });

    it('should emit syncRequested, but not cardClicked, when the sync button is clicked', () => {
        fixture.detectChanges();

        let syncCount = 0;
        let clickCount = 0;
        component.syncRequested.subscribe(() => syncCount++);
        component.cardClicked.subscribe(() => clickCount++);

        fixture.nativeElement.querySelector('.sync-button').click();

        expect(syncCount).toBe(1);
        expect(clickCount).toBe(0);
    });
});
