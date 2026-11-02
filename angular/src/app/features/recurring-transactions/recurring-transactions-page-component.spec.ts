import { describe, it, expect, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { RecurringTransactionsPageComponent } from './recurring-transactions-page-component';
import { RecurringTransactionModalComponent } from './recurring-transaction-modal';
import { RecurringTransactionService } from './recurring-transactions-service';
import { RecurringTransactionResponseInterface } from './interfaces';
import {
    CurrencyCodesEnum,
    RecurrenceIntervalEnum,
    RecurringTransactionStatusEnum,
    TransactionTypeEnum,
    WalletTypesEnum,
} from '@shared/enums';

/**
 * A MatDialog.open() valós helyett használt dialogRef, amivel az afterClosed() eredménye
 * kézzel szimulálható
 */
class FakeDialogRef {
    private closedSubject = new Subject<boolean | undefined>();

    close(result?: boolean): void {
        this.closedSubject.next(result);
        this.closedSubject.complete();
    }

    afterClosed(): Observable<boolean | undefined> {
        return this.closedSubject.asObservable();
    }
}

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

describe('RecurringTransactionsPageComponent (Vitest)', () => {
    let fixture: ComponentFixture<RecurringTransactionsPageComponent>;
    let listSpy: ReturnType<typeof vi.fn>;
    let dialogOpenSpy: ReturnType<typeof vi.fn>;
    let dialogRefs: FakeDialogRef[];

    async function setup(
        listResult: Observable<RecurringTransactionResponseInterface[]> = of([rent, pocketMoney]),
        waitForStable = true,
    ) {
        listSpy = vi.fn(() => listResult);
        dialogRefs = [];
        dialogOpenSpy = vi.fn(() => {
            const dialogRef = new FakeDialogRef();
            dialogRefs.push(dialogRef);
            return dialogRef;
        });

        TestBed.configureTestingModule({
            imports: [RecurringTransactionsPageComponent],
            providers: [
                provideTranslateService(),
                {
                    provide: RecurringTransactionService,
                    useValue: { listRecurringTransactions: listSpy },
                },
                { provide: MatDialog, useValue: { open: dialogOpenSpy } },
            ],
        });
        // hu locale-ban a pénznem szimbóluma az összeg mögé kerül (pl. "150 000 Ft")
        TestBed.inject(TranslateService).use('hu');

        fixture = TestBed.createComponent(RecurringTransactionsPageComponent);
        fixture.detectChanges();
        // függőben lévő resource mellett a whenStable() nem térne vissza
        if (waitForStable) {
            await fixture.whenStable();
            fixture.detectChanges();
        }
    }

    function rows(): HTMLElement[] {
        return Array.from(fixture.nativeElement.querySelectorAll('.recurring-row'));
    }

    function text(element: Element | null): string {
        return element?.textContent?.replace(/\s+/g, ' ').trim() ?? '';
    }

    function openedModalData(): unknown {
        const [componentType, config] = dialogOpenSpy.mock.calls.at(-1) as [
            unknown,
            { data: unknown },
        ];
        expect(componentType).toBe(RecurringTransactionModalComponent);
        return config.data;
    }

    it('should show a spinner while the list is loading', async () => {
        await setup(new Subject<RecurringTransactionResponseInterface[]>().asObservable(), false);

        expect(fixture.nativeElement.querySelector('mat-spinner')).not.toBeNull();
        expect(rows().length).toBe(0);
    });

    it('should render a row for every recurring transaction', async () => {
        await setup();

        expect(rows().length).toBe(2);
        expect(text(rows()[0]!.querySelector('.name'))).toBe('Albérlet');
        expect(text(rows()[0]!.querySelector('.wallet-name'))).toBe('(Napi költés)');
        expect(text(rows()[0]!.querySelector('.categories'))).toBe('Rezsi, Lakás');
    });

    it('should render an expense with the outcome style, its price and interval', async () => {
        await setup();
        const price = rows()[0]!.querySelector('.price')!;

        expect(price.classList.contains('outcome')).toBe(true);
        expect(price.classList.contains('income')).toBe(false);
        expect(text(price)).toBe('-150 000 Ft / recurring_transaction.interval.MONTHLY');
    });

    it('should render an income with the income style', async () => {
        await setup();
        const price = rows()[1]!.querySelector('.price')!;

        expect(price.classList.contains('income')).toBe(true);
        expect(text(price)).toContain('recurring_transaction.interval.WEEKLY');
    });

    it('should not render the categories line when there are no categories', async () => {
        await setup();

        expect(rows()[1]!.querySelector('.categories')).toBeNull();
    });

    it('should show the next occurrence only for an active recurring transaction', async () => {
        await setup();

        expect(text(rows()[0]!.querySelector('.status'))).toBe(
            'recurring_transaction.status.ACTIVE',
        );
        expect(text(rows()[0]!.querySelector('.date'))).toContain('2026-12-01');

        const stoppedStatus = rows()[1]!.querySelector('.status')!;
        expect(text(stoppedStatus)).toBe('recurring_transaction.status.STOPPED');
        expect(stoppedStatus.classList.contains('stopped')).toBe(true);
        expect(rows()[1]!.querySelector('.date')).toBeNull();
    });

    it('should show the empty message when there are no recurring transactions', async () => {
        await setup(of([]));

        expect(rows().length).toBe(0);
        expect(text(fixture.nativeElement.querySelector('.recurring-list'))).toBe(
            'recurring_transaction.empty',
        );
    });

    it('should show a general error when loading the list fails', async () => {
        await setup(throwError(() => new Error('network')));

        expect(rows().length).toBe(0);
        expect(text(fixture.nativeElement.querySelector('.recurring-list'))).toBe(
            'etc.general-error',
        );
    });

    it('should open an empty modal when clicking the create button', async () => {
        await setup();

        fixture.nativeElement.querySelector('.actions button').click();

        expect(openedModalData()).toEqual({ recurringTransaction: null });
    });

    it('should open the modal with the recurring transaction when clicking its row', async () => {
        await setup();

        rows()[1]!.click();

        expect(openedModalData()).toEqual({ recurringTransaction: pocketMoney });
    });

    it('should open the modal when pressing enter on a row', async () => {
        await setup();

        rows()[0]!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

        expect(openedModalData()).toEqual({ recurringTransaction: rent });
    });

    it('should open the modal when pressing space on a row', async () => {
        await setup();

        rows()[0]!.dispatchEvent(new KeyboardEvent('keydown', { key: ' ' }));

        expect(openedModalData()).toEqual({ recurringTransaction: rent });
    });

    it('should reload the list after the modal is closed with a successful save', async () => {
        await setup();
        rows()[0]!.click();

        dialogRefs.at(-1)!.close(true);
        await fixture.whenStable();

        expect(listSpy).toHaveBeenCalledTimes(2);
    });

    it('should not reload the list when the modal is closed without saving', async () => {
        await setup();
        rows()[0]!.click();

        dialogRefs.at(-1)!.close(undefined);
        await fixture.whenStable();

        expect(listSpy).toHaveBeenCalledTimes(1);
    });
});
