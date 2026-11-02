import { describe, it, expect, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { RecurringTransactionRowComponent } from './recurring-transaction-row-component';
import { RecurringTransactionResponseInterface } from './interfaces';
import {
    CurrencyCodesEnum,
    RecurrenceIntervalEnum,
    RecurringTransactionStatusEnum,
    TransactionTypeEnum,
    WalletTypesEnum,
} from '@shared/enums';

const rent: RecurringTransactionResponseInterface = {
    id: 5,
    name: 'Albérlet',
    priceSum: -150000,
    startDate: '2026-01-01',
    nextOccurrenceDate: '2026-12-01',
    recurrenceInterval: RecurrenceIntervalEnum.MONTHLY,
    transactionType: TransactionTypeEnum.OUTCOME,
    status: RecurringTransactionStatusEnum.ACTIVE,
    categories: [
        { id: 10, name: 'Rezsi', isDefaultCategory: false },
        { id: 11, name: 'Lakás', isDefaultCategory: false },
    ],
    wallet: {
        id: 1,
        name: 'Napi költés',
        currencyCode: CurrencyCodesEnum.huf,
        type: WalletTypesEnum.default,
    },
};

const pocketMoney: RecurringTransactionResponseInterface = {
    ...rent,
    id: 6,
    name: 'Zsebpénz',
    priceSum: 5000,
    nextOccurrenceDate: '2026-11-09',
    recurrenceInterval: RecurrenceIntervalEnum.WEEKLY,
    transactionType: TransactionTypeEnum.INCOME,
    status: RecurringTransactionStatusEnum.STOPPED,
    categories: [],
};

describe('RecurringTransactionRowComponent (Vitest)', () => {
    let fixture: ComponentFixture<RecurringTransactionRowComponent>;
    let selectedSpy: ReturnType<typeof vi.fn<() => void>>;

    function setup(transaction: RecurringTransactionResponseInterface) {
        TestBed.configureTestingModule({
            imports: [RecurringTransactionRowComponent],
            providers: [provideTranslateService()],
        });
        // hu locale-ban a pénznem szimbóluma az összeg mögé kerül (pl. "150 000 Ft")
        TestBed.inject(TranslateService).use('hu');

        fixture = TestBed.createComponent(RecurringTransactionRowComponent);
        fixture.componentRef.setInput('transaction', transaction);
        selectedSpy = vi.fn<() => void>();
        fixture.componentInstance.selected.subscribe(selectedSpy);
        fixture.detectChanges();
    }

    function host(): HTMLElement {
        return fixture.nativeElement;
    }

    function text(element: Element | null): string {
        return element?.textContent?.replace(/\s+/g, ' ').trim() ?? '';
    }

    it('should render the name, wallet and categories', () => {
        setup(rent);

        expect(text(host().querySelector('.name'))).toBe('Albérlet');
        expect(text(host().querySelector('.wallet-name'))).toBe('(Napi költés)');
        expect(text(host().querySelector('.categories'))).toBe('Rezsi, Lakás');
    });

    it('should render an expense with the outcome style, its price and interval', () => {
        setup(rent);
        const price = host().querySelector('.price')!;

        expect(price.classList.contains('outcome')).toBe(true);
        expect(price.classList.contains('income')).toBe(false);
        expect(text(price)).toBe('-150 000 Ft / recurring_transaction.interval.monthly');
    });

    it('should render an income with the income style', () => {
        setup(pocketMoney);
        const price = host().querySelector('.price')!;

        expect(price.classList.contains('income')).toBe(true);
        expect(text(price)).toContain('recurring_transaction.interval.weekly');
    });

    it('should not render the categories line when there are no categories', () => {
        setup(pocketMoney);

        expect(host().querySelector('.categories')).toBeNull();
    });

    it('should show the next occurrence for an active recurring transaction', () => {
        setup(rent);

        expect(text(host().querySelector('.status'))).toBe('recurring_transaction.status.active');
        expect(host().querySelector('.status')!.classList.contains('stopped')).toBe(false);
        expect(text(host().querySelector('.date'))).toContain('2026-12-01');
    });

    it('should not show the next occurrence for a stopped recurring transaction', () => {
        setup(pocketMoney);
        const status = host().querySelector('.status')!;

        expect(text(status)).toBe('recurring_transaction.status.stopped');
        expect(status.classList.contains('stopped')).toBe(true);
        expect(host().querySelector('.date')).toBeNull();
    });

    it('should render the deleted status label', () => {
        setup({ ...rent, status: RecurringTransactionStatusEnum.DELETED });
        const status = host().querySelector('.status')!;

        expect(text(status)).toBe('recurring_transaction.status.deleted');
        expect(status.classList.contains('stopped')).toBe(true);
    });

    it('should be focusable as a button labelled with the name', () => {
        setup(rent);

        expect(host().getAttribute('role')).toBe('button');
        expect(host().getAttribute('tabindex')).toBe('0');
        expect(host().getAttribute('aria-label')).toBe('Albérlet');
    });

    it('should emit selected on click', () => {
        setup(rent);

        host().click();

        expect(selectedSpy).toHaveBeenCalledTimes(1);
    });

    it('should emit selected on enter', () => {
        setup(rent);

        host().dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

        expect(selectedSpy).toHaveBeenCalledTimes(1);
    });

    it('should emit selected on space and prevent page scrolling', () => {
        setup(rent);
        const event = new KeyboardEvent('keydown', { key: ' ', cancelable: true });

        host().dispatchEvent(event);

        expect(selectedSpy).toHaveBeenCalledTimes(1);
        expect(event.defaultPrevented).toBe(true);
    });
});
