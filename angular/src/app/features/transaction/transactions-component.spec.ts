import { describe, it, expect, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EventEmitter } from '@angular/core';
import { BehaviorSubject, Observable, of, Subject, throwError } from 'rxjs';
import { provideTranslateService } from '@ngx-translate/core';
import { ActivatedRoute, Router } from '@angular/router';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatDialog } from '@angular/material/dialog';
import { TransactionsListComponent } from './transactions-component';
import { TransactionService } from './transaction-service';
import { CategoryService } from './category-service';
import { TransactionListElementData } from '../transaction-list/interfaces';
import { TransactionModalStateService } from './transaction-modal-state-service';
import { TransactionModalComponent } from './transaction-modal';
import { CurrencyCodesEnum, TransactionTypeEnum, WalletTypesEnum } from '../../shared/enums';

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

const sampleTransactions: TransactionListElementData[] = [
    {
        id: 1,
        name: 'Fizetés',
        priceSum: 500000,
        transactionType: TransactionTypeEnum.INCOME,
        transactionDate: '2024-01-10',
        isComplexTransaction: false,
        transactionDetails: [],
        wallet: {
            id: 1,
            name: 'Napi költés',
            currencyCode: CurrencyCodesEnum.huf,
            type: WalletTypesEnum.default,
        },
    },
];

describe('TransactionsListComponent (Vitest)', () => {
    let fixture: ComponentFixture<TransactionsListComponent>;
    let component: TransactionsListComponent;
    let transactionServiceMock: {
        getLastTransactions: ReturnType<typeof vi.fn>;
        getTransactionHistory: ReturnType<typeof vi.fn>;
        getTransactionById: ReturnType<typeof vi.fn>;
    };
    let routerMock: { navigate: ReturnType<typeof vi.fn> };
    let queryParamsSubject: BehaviorSubject<Record<string, string>>;
    let dialogOpenSpy: ReturnType<typeof vi.fn>;
    let transactionModalDialogRefs: FakeDialogRef<{
        deleteTransactionRequested: EventEmitter<number>;
        saved: EventEmitter<unknown>;
        categoryAdded: EventEmitter<string>;
    }>[];

    function setup(options: {
        isHistoryMode?: boolean;
        needSearchField?: boolean;
        queryParams?: Record<string, string>;
        getLastTransactionsResult?: ReturnType<typeof of> | ReturnType<typeof throwError>;
    } = {}) {
        transactionServiceMock = {
            getLastTransactions: vi.fn(() => options.getLastTransactionsResult ?? of(sampleTransactions)),
            getTransactionHistory: vi.fn(() => of(sampleTransactions)),
            getTransactionById: vi.fn(() => of(null)),
        };
        routerMock = { navigate: vi.fn() };
        queryParamsSubject = new BehaviorSubject<Record<string, string>>(options.queryParams ?? {});

        transactionModalDialogRefs = [];
        dialogOpenSpy = vi.fn((componentType: unknown) => {
            if (componentType === TransactionModalComponent) {
                const dialogRef = new FakeDialogRef({
                    deleteTransactionRequested: new EventEmitter<number>(),
                    saved: new EventEmitter<unknown>(),
                    categoryAdded: new EventEmitter<string>(),
                });
                transactionModalDialogRefs.push(dialogRef);
                return dialogRef;
            }
            throw new Error('Unexpected dialog component opened: ' + String(componentType));
        });

        TestBed.configureTestingModule({
            imports: [TransactionsListComponent],
            providers: [
                provideTranslateService(),
                provideNativeDateAdapter(),
                { provide: TransactionService, useValue: transactionServiceMock },
                {
                    provide: CategoryService,
                    useValue: {
                        listCategories: () => of([]),
                        saveCategory: () => of({ id: 1, name: 'x', isDefaultCategory: false }),
                    },
                },
                { provide: Router, useValue: routerMock },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        snapshot: { queryParams: options.queryParams ?? {} },
                        queryParams: queryParamsSubject,
                    },
                },
                { provide: MatDialog, useValue: { open: dialogOpenSpy } },
            ],
        });

        fixture = TestBed.createComponent(TransactionsListComponent);
        component = fixture.componentInstance;
        component.isHistoryMode = options.isHistoryMode ?? false;
        component.needSearchField = options.needSearchField ?? false;
        fixture.detectChanges();
    }

    it('should load the last transactions on init when not in history mode', () => {
        setup({ isHistoryMode: false });

        expect(transactionServiceMock.getLastTransactions).toHaveBeenCalledTimes(1);
        expect(transactionServiceMock.getTransactionHistory).not.toHaveBeenCalled();

        const list = fixture.nativeElement.querySelector('app-transaction-list');
        expect(list.querySelectorAll('app-transaction-card').length).toBe(1);
    });

    it('should stop the loading state and keep the list empty if fetching the last transactions fails', () => {
        setup({ isHistoryMode: false, getLastTransactionsResult: throwError(() => new Error('boom')) });

        expect(component['isTransactionListLoading']()).toBe(false);
        const list = fixture.nativeElement.querySelector('app-transaction-list');
        expect(list.querySelectorAll('app-transaction-card').length).toBe(0);
    });

    it('should initialize the filter form from the query params in history mode', () => {
        setup({
            isHistoryMode: true,
            needSearchField: true,
            queryParams: { name: 'kávé' },
        });

        const nameInput = fixture.nativeElement.querySelector('#name');
        expect(nameInput.value).toBe('kávé');
        expect(transactionServiceMock.getTransactionHistory).toHaveBeenCalledTimes(1);
        const [params] = transactionServiceMock.getTransactionHistory.mock.calls[0] as [URLSearchParams];
        expect(params.get('name')).toBe('kávé');
    });

    it('should navigate with the entered filter values, replacing (not merging) the query params, when the filter form is submitted', () => {
        setup({ isHistoryMode: true, needSearchField: true });
        routerMock.navigate.mockClear();

        const nameInput = fixture.nativeElement.querySelector('#name');
        nameInput.value = 'tej';
        nameInput.dispatchEvent(new Event('input'));

        const form = fixture.nativeElement.querySelector('form');
        form.dispatchEvent(new Event('submit'));

        expect(routerMock.navigate).toHaveBeenCalledWith(
            [],
            expect.objectContaining({ queryParams: { name: 'tej' } }),
        );
        expect(routerMock.navigate).not.toHaveBeenCalledWith(
            [],
            expect.objectContaining({ queryParamsHandling: 'merge' }),
        );
    });

    it('should reload the history whenever the route query params change (e.g. after the filter navigates)', () => {
        setup({ isHistoryMode: true, needSearchField: true });
        transactionServiceMock.getTransactionHistory.mockClear();

        queryParamsSubject.next({ name: 'tej' });

        expect(transactionServiceMock.getTransactionHistory).toHaveBeenCalledTimes(1);
        const [params] = transactionServiceMock.getTransactionHistory.mock.calls[0] as [URLSearchParams];
        expect(params.get('name')).toBe('tej');
    });

    it('should reset the form and navigate with empty query params when clearInputs is called', () => {
        setup({ isHistoryMode: true, needSearchField: true, queryParams: { name: 'kávé' } });
        transactionServiceMock.getTransactionHistory.mockClear();
        routerMock.navigate.mockClear();

        const nameInput = fixture.nativeElement.querySelector('#name');
        expect(nameInput.value).toBe('kávé');

        const clearButton = fixture.nativeElement.querySelector('#clear-filters');
        clearButton.click();
        fixture.detectChanges();

        expect(nameInput.value).toBe('');
        expect(routerMock.navigate).toHaveBeenCalledWith(
            [],
            expect.objectContaining({ queryParams: {} }),
        );

        // a router.navigate mockolt, ezért a valós navigáció eredményét (a route queryParams
        // frissülését) itt szimuláljuk, hogy a reaktív újratöltést is leteszteljük
        queryParamsSubject.next({});
        expect(transactionServiceMock.getTransactionHistory).toHaveBeenCalledTimes(1);
        const [params] = transactionServiceMock.getTransactionHistory.mock.calls[0] as [URLSearchParams];
        expect(params.get('name')).toBeNull();
    });

    it('should reload the list once when reloadTrigger changes after the initial render', () => {
        setup({ isHistoryMode: false });

        // az input első explicit beállítása maga számít "firstChange"-nek Angular szemszögéből,
        // függetlenül a mező JS-alapértékétől, ezért ez még nem tölt újra
        fixture.componentRef.setInput('reloadTrigger', 1);
        fixture.detectChanges();
        transactionServiceMock.getLastTransactions.mockClear();

        fixture.componentRef.setInput('reloadTrigger', 2);
        fixture.detectChanges();

        expect(transactionServiceMock.getLastTransactions).toHaveBeenCalledTimes(1);
    });

    it('should not reload again on the initial reloadTrigger change (firstChange guard)', () => {
        setup({ isHistoryMode: false });

        // ngOnChanges runs once more right after ngOnInit for the same initial value; must stay a no-op
        component.ngOnChanges({
            reloadTrigger: {
                currentValue: 0,
                previousValue: 0,
                firstChange: true,
                isFirstChange: () => true,
            },
        });

        expect(transactionServiceMock.getLastTransactions).toHaveBeenCalledTimes(1);
    });

    it('should reload the list and emit transactionsChanged when the modal reports a save/delete', () => {
        setup({ isHistoryMode: false });
        transactionServiceMock.getLastTransactions.mockClear();

        let emitted = false;
        component.transactionsChanged.subscribe(() => (emitted = true));

        const modalState = fixture.debugElement.injector.get(TransactionModalStateService);
        modalState.changed.next();

        expect(transactionServiceMock.getLastTransactions).toHaveBeenCalledTimes(1);
        expect(emitted).toBe(true);
    });

    it('should open the modal with the clicked transaction id when a card is clicked', () => {
        setup({ isHistoryMode: false });

        const card = fixture.nativeElement.querySelector('app-transaction-card');
        card.click();
        fixture.detectChanges();

        expect(transactionServiceMock.getTransactionById).toHaveBeenCalledWith(1);
        expect(dialogOpenSpy).toHaveBeenCalledWith(TransactionModalComponent, expect.anything());
        expect(transactionModalDialogRefs).toHaveLength(1);
    });
});
