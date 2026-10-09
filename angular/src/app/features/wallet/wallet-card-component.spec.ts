import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
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

    afterEach(() => {
        vi.useRealTimers();
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

    /**
     * Csak a Date-et fakeljük, hogy az Angular időzítői ne álljanak meg.
     * Dél, hogy a napváltás és az időzóna ne befolyásolja az eredményt
     */
    function setToday(year: number, month: number, day: number): void {
        vi.useFakeTimers({ toFake: ['Date'] });
        vi.setSystemTime(new Date(year, month - 1, day, 12, 0, 0));
    }

    function syncWarningIcon(): HTMLElement | null {
        return fixture.nativeElement.querySelector('.sync-button mat-icon');
    }

    function syncButton(): HTMLButtonElement {
        return fixture.nativeElement.querySelector('.sync-button');
    }

    it('should not show the sync warning when the last sync was exactly 30 days ago', () => {
        // 2026-09-15 + 30 nap
        setToday(2026, 10, 15);
        fixture.detectChanges();

        expect(syncWarningIcon()).toBeNull();
        expect(syncButton().classList).not.toContain('mat-button-danger');
        expect(syncButton().querySelector('.cdk-visually-hidden')).toBeNull();
    });

    it('should show the sync warning in red, with a screen reader text, when the last sync was more than 30 days ago', () => {
        setToday(2026, 10, 16);
        fixture.detectChanges();

        expect(syncWarningIcon()).not.toBeNull();
        expect(syncButton().classList).toContain('mat-button-danger');
        expect(syncButton().querySelector('.cdk-visually-hidden')?.textContent).toContain(
            'wallet.sync.outdated',
        );
    });

    it('should not show the sync warning when the wallet was synced today', () => {
        setToday(2026, 9, 15);
        fixture.detectChanges();

        expect(syncWarningIcon()).toBeNull();
    });

    // a 2026-10-25-i téli óraátállítás miatt ez a 31 nap valójában 31 nap + 1 óra
    it('should count whole days across a daylight saving time change', () => {
        fixture.componentRef.setInput('walletData', { ...wallet, lastSyncDate: '2026-10-01' });
        setToday(2026, 11, 1);
        fixture.detectChanges();

        expect(syncWarningIcon()).not.toBeNull();
    });
});
