import { describe, it, expect, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { NotificationService } from '@shared/services/notification-service';
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
    let getByIdSpy: ReturnType<typeof vi.fn>;
    let dialogOpenSpy: ReturnType<typeof vi.fn>;
    let dialogRefs: FakeDialogRef[];
    let routerMock: { navigate: ReturnType<typeof vi.fn> };
    let notificationMock: {
        show: ReturnType<typeof vi.fn>;
        showGeneralError: ReturnType<typeof vi.fn>;
    };

    async function setup(
        listResult: Observable<RecurringTransactionResponseInterface[]> = of([rent, pocketMoney]),
        waitForStable = true,
        options: {
            routeParams?: Record<string, string>;
            getByIdResult?: Observable<RecurringTransactionResponseInterface>;
        } = {},
    ) {
        listSpy = vi.fn(() => listResult);
        getByIdSpy = vi.fn(() => options.getByIdResult ?? of(rent));
        dialogRefs = [];
        dialogOpenSpy = vi.fn(() => {
            const dialogRef = new FakeDialogRef();
            dialogRefs.push(dialogRef);
            return dialogRef;
        });
        routerMock = { navigate: vi.fn(() => Promise.resolve(true)) };
        notificationMock = { show: vi.fn(), showGeneralError: vi.fn() };
        const routeParams = options.routeParams ?? {};

        TestBed.configureTestingModule({
            imports: [RecurringTransactionsPageComponent],
            providers: [
                provideTranslateService(),
                {
                    provide: RecurringTransactionService,
                    useValue: {
                        listRecurringTransactions: listSpy,
                        getRecurringTransactionById: getByIdSpy,
                        // üres statisztika: a diagram az üres állapotot mutatja, így a
                        // tesztkörnyezetben (canvas nélkül) nem rajzol
                        recurringTransactionsStat: () => of([]),
                    },
                },
                { provide: MatDialog, useValue: { open: dialogOpenSpy } },
                { provide: Router, useValue: routerMock },
                {
                    provide: ActivatedRoute,
                    useValue: { snapshot: { params: routeParams }, params: of(routeParams) },
                },
                { provide: NotificationService, useValue: notificationMock },
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
        return Array.from(
            fixture.nativeElement.querySelectorAll('app-recurring-transaction-row-component'),
        );
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

    // A sor tartalmát a recurring-transaction-row-component.spec.ts teszteli
    it('should render a row for every recurring transaction in order', async () => {
        await setup();

        expect(rows().length).toBe(2);
        expect(text(rows()[0]!.querySelector('.name'))).toBe('Albérlet');
        expect(text(rows()[1]!.querySelector('.name'))).toBe('Zsebpénz');
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

    it('should not open the modal nor navigate when there is no id route param', async () => {
        await setup();

        expect(getByIdSpy).not.toHaveBeenCalled();
        expect(dialogOpenSpy).not.toHaveBeenCalled();
        expect(routerMock.navigate).not.toHaveBeenCalled();
    });

    it('should load the recurring transaction and open its modal when the url contains its id', async () => {
        await setup(undefined, true, { routeParams: { id: '5' }, getByIdResult: of(rent) });

        expect(getByIdSpy).toHaveBeenCalledWith(5);
        expect(openedModalData()).toEqual({ recurringTransaction: rent });
        expect(routerMock.navigate).not.toHaveBeenCalled();
    });

    it.each(['abc', '0', '-1', '1.5'])(
        'should navigate back to the list without loading nor opening the modal when the id route param is invalid (%s)',
        async (id) => {
            await setup(undefined, true, { routeParams: { id } });

            expect(getByIdSpy).not.toHaveBeenCalled();
            expect(dialogOpenSpy).not.toHaveBeenCalled();
            expect(routerMock.navigate).toHaveBeenCalledWith(['/recurring-transactions'], {
                replaceUrl: true,
            });
        },
    );

    it('should show a not found message and navigate back to the list when the recurring transaction does not exist', async () => {
        await setup(undefined, true, {
            routeParams: { id: '99' },
            getByIdResult: throwError(() => new HttpErrorResponse({ status: 404 })),
        });

        expect(dialogOpenSpy).not.toHaveBeenCalled();
        expect(notificationMock.show).toHaveBeenCalledWith('recurring_transaction.not_found');
        expect(routerMock.navigate).toHaveBeenCalledWith(['/recurring-transactions'], {
            replaceUrl: true,
        });
    });

    it('should show a general error and navigate back to the list when loading the recurring transaction fails', async () => {
        await setup(undefined, true, {
            routeParams: { id: '5' },
            getByIdResult: throwError(() => new HttpErrorResponse({ status: 500 })),
        });

        expect(dialogOpenSpy).not.toHaveBeenCalled();
        expect(notificationMock.showGeneralError).toHaveBeenCalled();
        expect(routerMock.navigate).toHaveBeenCalledWith(['/recurring-transactions'], {
            replaceUrl: true,
        });
    });

    it('should remove the id from the url when the modal opened from the route is closed', async () => {
        await setup(undefined, true, { routeParams: { id: '5' }, getByIdResult: of(rent) });
        expect(routerMock.navigate).not.toHaveBeenCalled();

        dialogRefs.at(-1)!.close(undefined);

        expect(routerMock.navigate).toHaveBeenCalledWith(['/recurring-transactions'], {
            replaceUrl: true,
        });
    });

    it('should not navigate when a modal opened from the list (without id route param) is closed', async () => {
        await setup();
        rows()[0]!.click();

        dialogRefs.at(-1)!.close(true);

        expect(routerMock.navigate).not.toHaveBeenCalled();
    });
});
