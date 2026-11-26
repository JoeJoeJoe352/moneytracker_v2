import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, EventEmitter, input, output } from '@angular/core';
import { Observable, Subject, of } from 'rxjs';
import { provideTranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { MainPageComponent } from './main-page-component';
import { TransactionService } from '../transaction/transaction-service';
import { CategoryService } from '../transaction/category-service';
import { MoneySumInterface, NavigationDataToRecurringTransaction } from '../transaction/interfaces';
import { TransactionModalStateService } from '../transaction/transaction-modal-state-service';
import { TransactionModalComponent } from '../transaction/transaction-modal';
import { TransferTransactionModalComponent } from '../transaction/transfer-transaction/transfer-transaction-modal';
import { CurrencyCodesEnum } from '@shared/enums';
import { StatCardComponent } from './stat-card-component';
import { BalanceCardComponent } from './balance-card-component';

@Component({
    selector: 'app-transactions-list-component',
    template: '',
})
class StubTransactionsListComponent {
    public isHistoryMode = input(false);
    public needSearchField = input(false);
    public reloadTrigger = input(0);
    public transactionsChanged = output();
}

/**
 * A MatDialog.open() valós helyett használt, kézzel vezérelhető dialogRef, ami lehetővé teszi a
 * dialog "componentInstance"-ének (kimenő eseményeinek) és afterClosed()-jének szimulálását,
 * anélkül hogy a valós MatDialog/CDK overlay-t kellene betöltenünk a tesztekhez.
 */
class FakeDialogRef<T> {
    close = vi.fn((result?: unknown) => this.closedSubject.next(result));
    private closedSubject = new Subject<unknown>();

    constructor(public componentInstance: T) {}

    afterClosed(): Observable<unknown> {
        return this.closedSubject.asObservable();
    }
}

describe('MainPageComponent (Vitest)', () => {
    let fixture: ComponentFixture<MainPageComponent>;
    let getMoneySum$: Subject<MoneySumInterface>;
    let getMoneySumSpy: ReturnType<typeof vi.fn>;
    let dialogOpenSpy: ReturnType<typeof vi.fn>;
    let transactionModalDialogRefs: FakeDialogRef<{
        deleteTransactionRequested: EventEmitter<number>;
        saved: EventEmitter<unknown>;
        recurringTransactionOpenRequest: EventEmitter<NavigationDataToRecurringTransaction>;
    }>[];
    let transferModalDialogRefs: FakeDialogRef<object>[];

    function setup() {
        getMoneySum$ = new Subject<MoneySumInterface>();
        getMoneySumSpy = vi.fn(() => getMoneySum$.asObservable());

        transactionModalDialogRefs = [];
        transferModalDialogRefs = [];
        dialogOpenSpy = vi.fn((componentType: unknown) => {
            if (componentType === TransferTransactionModalComponent) {
                const dialogRef = new FakeDialogRef({});
                transferModalDialogRefs.push(dialogRef);
                return dialogRef;
            }
            if (componentType === TransactionModalComponent) {
                const dialogRef = new FakeDialogRef({
                    deleteTransactionRequested: new EventEmitter<number>(),
                    saved: new EventEmitter<unknown>(),
                    recurringTransactionOpenRequest:
                        new EventEmitter<NavigationDataToRecurringTransaction>(),
                });
                transactionModalDialogRefs.push(dialogRef);
                return dialogRef;
            }
            throw new Error('Unexpected dialog component opened: ' + String(componentType));
        });

        TestBed.configureTestingModule({
            imports: [MainPageComponent],
            providers: [
                provideTranslateService(),
                {
                    provide: TransactionService,
                    useValue: {
                        getMoneySum: getMoneySumSpy,
                        getLastTransactions: () => of({ transactions: [] }),
                        getTransactionHistory: () =>
                            of({
                                transactions: [],
                                pageData: { elementCount: 0, elementPerPage: 15 },
                            }),
                        getTransactionById: () => of(null),
                    },
                },
                {
                    provide: CategoryService,
                    useValue: {
                        listCategories: () => of([]),
                        saveCategory: () => of({ id: 1, name: 'x', isDefaultCategory: false }),
                    },
                },
                { provide: MatDialog, useValue: { open: dialogOpenSpy } },
            ],
        });
        TestBed.overrideComponent(MainPageComponent, {
            set: {
                imports: [StubTransactionsListComponent, StatCardComponent, BalanceCardComponent],
            },
        });

        fixture = TestBed.createComponent(MainPageComponent);
        fixture.detectChanges();
    }

    beforeEach(() => setup());

    it('should show a spinner while the money sum is loading, then render the totals once loaded', async () => {
        expect(fixture.nativeElement.querySelector('.balance-card mat-spinner')).toBeTruthy();
        expect(fixture.nativeElement.querySelector('.balance-card .stat-value')).toBeNull();

        getMoneySum$.next({
            moneySum: [{ currencyCode: CurrencyCodesEnum.huf, total: 15000 }],
            incomeSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 5000 }],
            expenseSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 2000 }],
        });
        fixture.detectChanges();
        await fixture.whenStable();

        expect(fixture.nativeElement.querySelector('.balance-card mat-spinner')).toBeNull();
        expect(
            fixture.nativeElement.querySelector('.balance-card .stat-value').textContent,
        ).toContain('15,000');
        expect(fixture.nativeElement.querySelector('.stat-value.income').textContent).toContain(
            '5,000',
        );
        expect(fixture.nativeElement.querySelector('.stat-value.expense').textContent).toContain(
            '2,000',
        );
    });

    it('should render the balance trend as positive when income exceeds expense this month', async () => {
        getMoneySum$.next({
            moneySum: [{ currencyCode: CurrencyCodesEnum.huf, total: 3000 }],
            incomeSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 5000 }],
            expenseSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 2000 }],
        });
        fixture.detectChanges();
        await fixture.whenStable();

        const trend = fixture.nativeElement.querySelector('.stat-trend');
        expect(trend.classList.contains('positive')).toBe(true);
        expect(trend.classList.contains('negative')).toBe(false);
        expect(trend.querySelector('mat-icon[fontIcon="arrow_upward"]')).toBeTruthy();
        expect(trend.textContent).toContain('3,000');
    });

    it('should render the balance trend as negative when expense exceeds income this month', async () => {
        getMoneySum$.next({
            moneySum: [{ currencyCode: CurrencyCodesEnum.huf, total: -1000 }],
            incomeSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 1000 }],
            expenseSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 2000 }],
        });
        fixture.detectChanges();
        await fixture.whenStable();

        const trend = fixture.nativeElement.querySelector('.stat-trend');
        expect(trend.classList.contains('negative')).toBe(true);
        expect(trend.classList.contains('positive')).toBe(false);
        expect(trend.querySelector('mat-icon[fontIcon="arrow_downward"]')).toBeTruthy();
    });

    it('should open the transaction modal when the create-transaction button is clicked', () => {
        getMoneySum$.next({
            moneySum: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
            incomeSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
            expenseSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
        });
        fixture.detectChanges();

        expect(dialogOpenSpy).not.toHaveBeenCalled();

        const createButton = fixture.nativeElement.querySelector('.balance-card button');
        createButton.click();
        fixture.detectChanges();

        expect(dialogOpenSpy).toHaveBeenCalledWith(TransactionModalComponent, expect.anything());
        expect(transactionModalDialogRefs).toHaveLength(1);
    });

    it('should refetch only the money sum when the transaction list reports a change', async () => {
        getMoneySum$.next({
            moneySum: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
            incomeSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
            expenseSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
        });
        fixture.detectChanges();
        await fixture.whenStable();
        expect(getMoneySumSpy).toHaveBeenCalledTimes(1);

        const listStub = fixture.debugElement.query(
            (de) => de.componentInstance instanceof StubTransactionsListComponent,
        ).componentInstance as StubTransactionsListComponent;
        listStub.transactionsChanged.emit();
        fixture.detectChanges();
        TestBed.tick();

        expect(getMoneySumSpy).toHaveBeenCalledTimes(2);
        expect(listStub.reloadTrigger()).toBe(0); // csak az összesítés töltődik újra, a lista nem
    });

    it('should refetch both the money sum and bump the list reload trigger when the modal reports a save/delete', async () => {
        getMoneySum$.next({
            moneySum: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
            incomeSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
            expenseSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
        });
        fixture.detectChanges();
        await fixture.whenStable();
        expect(getMoneySumSpy).toHaveBeenCalledTimes(1);

        const listStub = fixture.debugElement.query(
            (de) => de.componentInstance instanceof StubTransactionsListComponent,
        ).componentInstance as StubTransactionsListComponent;
        expect(listStub.reloadTrigger()).toBe(0);

        const modalState = fixture.debugElement.injector.get(TransactionModalStateService);
        modalState.changed.next();
        fixture.detectChanges();
        TestBed.tick();

        expect(getMoneySumSpy).toHaveBeenCalledTimes(2);
        expect(listStub.reloadTrigger()).toBe(1);
    });

    it('should open the transfer modal when the transfer button is clicked', () => {
        const transferButton = fixture.nativeElement.querySelector(
            '.balance-card .transfer-button',
        ) as HTMLButtonElement;
        transferButton.click();
        fixture.detectChanges();

        expect(dialogOpenSpy).toHaveBeenCalledWith(
            TransferTransactionModalComponent,
            expect.anything(),
        );
        expect(transferModalDialogRefs).toHaveLength(1);
    });

    it('should reload the money sum and the list after a transfer is saved', async () => {
        getMoneySum$.next({
            moneySum: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
            incomeSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
            expenseSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
        });
        fixture.detectChanges();
        await fixture.whenStable();
        expect(getMoneySumSpy).toHaveBeenCalledTimes(1);

        const listStub = fixture.debugElement.query(
            (de) => de.componentInstance instanceof StubTransactionsListComponent,
        ).componentInstance as StubTransactionsListComponent;

        (fixture.nativeElement.querySelector('.transfer-button') as HTMLButtonElement).click();
        expect(transferModalDialogRefs).toHaveLength(1);
        transferModalDialogRefs[0]?.close(true);
        fixture.detectChanges();
        TestBed.tick();

        expect(getMoneySumSpy).toHaveBeenCalledTimes(2);
        expect(listStub.reloadTrigger()).toBe(1);
    });

    it('should not reload anything when the transfer modal is closed without saving', async () => {
        getMoneySum$.next({
            moneySum: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
            incomeSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
            expenseSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
        });
        fixture.detectChanges();
        await fixture.whenStable();

        const listStub = fixture.debugElement.query(
            (de) => de.componentInstance instanceof StubTransactionsListComponent,
        ).componentInstance as StubTransactionsListComponent;

        (fixture.nativeElement.querySelector('.transfer-button') as HTMLButtonElement).click();
        expect(transferModalDialogRefs).toHaveLength(1);
        transferModalDialogRefs[0]?.close(false);
        fixture.detectChanges();
        TestBed.tick();

        expect(getMoneySumSpy).toHaveBeenCalledTimes(1);
        expect(listStub.reloadTrigger()).toBe(0);
    });
});
