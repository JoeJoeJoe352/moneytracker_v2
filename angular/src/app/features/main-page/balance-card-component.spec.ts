import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { BalanceCardComponent } from './balance-card-component';
import { TransactionModalStateService } from '../transaction/transaction-modal-state-service';
import { WalletSummaryInterface } from '../transaction/interfaces';
import { CurrencyCodesEnum } from '@shared/enums';

describe('BalanceCardComponent (Vitest)', () => {
    let fixture: ComponentFixture<BalanceCardComponent>;
    let modalMock: { open: ReturnType<typeof vi.fn>; openTransfer: ReturnType<typeof vi.fn> };

    beforeEach(() => {
        modalMock = { open: vi.fn(), openTransfer: vi.fn() };

        TestBed.configureTestingModule({
            imports: [BalanceCardComponent],
            providers: [
                provideTranslateService(),
                { provide: TransactionModalStateService, useValue: modalMock },
            ],
        });

        fixture = TestBed.createComponent(BalanceCardComponent);
    });

    function render(data: {
        isMoneySumLoading?: boolean;
        moneySum?: WalletSummaryInterface[];
        incomeSumThisMonth?: WalletSummaryInterface[];
        expenseSumThisMonth?: WalletSummaryInterface[];
    }): void {
        fixture.componentRef.setInput('isMoneySumLoading', data.isMoneySumLoading ?? false);
        fixture.componentRef.setInput('moneySum', data.moneySum ?? []);
        fixture.componentRef.setInput('incomeSumThisMonth', data.incomeSumThisMonth ?? []);
        fixture.componentRef.setInput('expenseSumThisMonth', data.expenseSumThisMonth ?? []);
        fixture.detectChanges();
    }

    function trends(): HTMLElement[] {
        return Array.from(fixture.nativeElement.querySelectorAll('.stat-trend'));
    }

    function trendAt(index: number): HTMLElement {
        const trend = trends()[index];
        if (!trend) {
            throw new Error(`No trend rendered at index ${index}`);
        }
        return trend;
    }

    it('should show a spinner and no values while loading', () => {
        render({
            isMoneySumLoading: true,
            moneySum: [{ currencyCode: CurrencyCodesEnum.huf, total: 15000 }],
        });

        expect(fixture.nativeElement.querySelector('mat-spinner')).toBeTruthy();
        expect(fixture.nativeElement.querySelector('.stat-value')).toBeNull();
        expect(trends()).toHaveLength(0);
    });

    it('should render the total balance for every currency', () => {
        render({
            moneySum: [
                { currencyCode: CurrencyCodesEnum.huf, total: 15000 },
                { currencyCode: CurrencyCodesEnum.eur, total: 250 },
            ],
        });

        const rows: string[] = Array.from(
            fixture.nativeElement.querySelectorAll('.stat-value > div'),
            (row: HTMLElement) => row.textContent,
        );
        expect(rows).toHaveLength(2);
        expect(rows[0]).toContain('15,000');
        expect(rows[1]).toContain('250');
    });

    it('should render no trend when there is no income and expense this month', () => {
        render({ moneySum: [{ currencyCode: CurrencyCodesEnum.huf, total: 15000 }] });

        expect(trends()).toHaveLength(0);
    });

    it('should render a positive trend when income exceeds expense', () => {
        render({
            incomeSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 5000 }],
            expenseSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 2000 }],
        });

        const trend = trendAt(0);
        expect(trends()).toHaveLength(1);
        expect(trend.classList.contains('positive')).toBe(true);
        expect(trend.querySelector('mat-icon[fontIcon="arrow_upward"]')).toBeTruthy();
        expect(trend.textContent).toContain('3,000');
    });

    it('should render a negative trend when expense exceeds income', () => {
        render({
            incomeSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 1000 }],
            expenseSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 2500 }],
        });

        const trend = trendAt(0);
        expect(trend.classList.contains('negative')).toBe(true);
        expect(trend.querySelector('mat-icon[fontIcon="arrow_downward"]')).toBeTruthy();
        expect(trend.textContent).toContain('1,500');
    });

    it('should render a zero change as a positive trend', () => {
        render({
            incomeSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 2000 }],
            expenseSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 2000 }],
        });

        const trend = trendAt(0);
        expect(trend.classList.contains('positive')).toBe(true);
        expect(trend.classList.contains('negative')).toBe(false);
    });

    it('should render one trend per currency when currencies appear only in income or only in expense', () => {
        render({
            incomeSumThisMonth: [
                { currencyCode: CurrencyCodesEnum.huf, total: 5000 },
                { currencyCode: CurrencyCodesEnum.eur, total: 100 },
            ],
            expenseSumThisMonth: [
                { currencyCode: CurrencyCodesEnum.huf, total: 1000 },
                { currencyCode: CurrencyCodesEnum.usd, total: 40 },
            ],
        });

        expect(trends()).toHaveLength(3);
        const huf = trendAt(0);
        const eur = trendAt(1);
        const usd = trendAt(2);

        // mindkét oldalon szereplő valuta: bevétel - kiadás
        expect(huf.classList.contains('positive')).toBe(true);
        expect(huf.textContent).toContain('4,000');

        // csak bevételben szereplő valuta: a teljes bevétel a változás
        expect(eur.classList.contains('positive')).toBe(true);
        expect(eur.textContent).toContain('100');

        // csak kiadásban szereplő valuta: a teljes kiadás negatív változásként jelenik meg
        expect(usd.classList.contains('negative')).toBe(true);
        expect(usd.textContent).toContain('40');
    });

    it('should open the transaction modal when the create-transaction button is clicked', () => {
        render({});

        (fixture.nativeElement.querySelector('.stat-action-button') as HTMLButtonElement).click();

        expect(modalMock.open).toHaveBeenCalledTimes(1);
        expect(modalMock.openTransfer).not.toHaveBeenCalled();
    });

    it('should open the transfer modal when the transfer button is clicked', () => {
        render({});

        (fixture.nativeElement.querySelector('.transfer-button') as HTMLButtonElement).click();

        expect(modalMock.openTransfer).toHaveBeenCalledTimes(1);
        expect(modalMock.open).not.toHaveBeenCalled();
    });

    it('should show the action buttons while loading', () => {
        render({ isMoneySumLoading: true });

        expect(fixture.nativeElement.querySelectorAll('.stat-action-button')).toHaveLength(2);
    });
});
