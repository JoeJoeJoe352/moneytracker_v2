import { describe, it, expect, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { of, throwError } from 'rxjs';
import { provideTranslateService } from '@ngx-translate/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { NgxsmkDatepickerComponent } from 'ngxsmk-datepicker';
import { TranslatePipe } from '@ngx-translate/core';
import TransactionListComponent from '../transaction-list/transaction-list-component';
import { TransactionsListComponent } from './transactions-component';
import { TransactionService } from './transaction-service';
import { CategoryService } from './category-service';
import { TransactionListElementData } from '../transaction-list/interfaces';
import { TransactionTypeEnum } from './transaction-type-enum';
import { TransactionModalStateService } from './transaction-modal-state-service';
import { CurrencyCodesEnum, WalletTypesEnum } from '../../shared/enums';

@Component({
    selector: 'app-create-transaction-modal',
    template: '',
})
class StubTransactionModalComponent {
    @Input() transaction: unknown = null;
    @Input() categories: unknown;
    @Input() isTransactionFormDisabled = false;
    @Input() isCategorySaveInProgress = false;
    @Input() isDataInitializing: unknown;
    @Output() closeModal = new EventEmitter<void>();
    @Output() deleteTransactionRequested = new EventEmitter<number>();
    @Output() saved = new EventEmitter<unknown>();
    @Output() categoryAdded = new EventEmitter<string>();
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

        TestBed.configureTestingModule({
            imports: [TransactionsListComponent],
            providers: [
                provideTranslateService(),
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
                    useValue: { snapshot: { queryParams: options.queryParams ?? {} } },
                },
            ],
        });
        TestBed.overrideComponent(TransactionsListComponent, {
            set: {
                imports: [
                    StubTransactionModalComponent,
                    TransactionListComponent,
                    ReactiveFormsModule,
                    NgxsmkDatepickerComponent,
                    TranslatePipe,
                ],
            },
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

        expect(component['filterForm'].value.name).toBe('kávé');
        expect(transactionServiceMock.getTransactionHistory).toHaveBeenCalledTimes(1);
        const [params] = transactionServiceMock.getTransactionHistory.mock.calls[0] as [URLSearchParams];
        expect(params.get('name')).toBe('kávé');
    });

    it('should navigate merging the used filter params into the URL after a successful history fetch', () => {
        setup({ isHistoryMode: true, needSearchField: true, queryParams: { name: 'kávé' } });

        expect(routerMock.navigate).toHaveBeenCalledWith(
            [],
            expect.objectContaining({
                queryParamsHandling: 'merge',
                queryParams: expect.objectContaining({ name: 'kávé' }),
            }),
        );
    });

    it('should reload the history with the current form value when the filter form is submitted', () => {
        setup({ isHistoryMode: true, needSearchField: true });
        transactionServiceMock.getTransactionHistory.mockClear();

        const nameInput = fixture.nativeElement.querySelector('#name');
        nameInput.value = 'tej';
        nameInput.dispatchEvent(new Event('input'));

        const form = fixture.nativeElement.querySelector('form');
        form.dispatchEvent(new Event('submit'));

        expect(transactionServiceMock.getTransactionHistory).toHaveBeenCalledTimes(1);
        const [params] = transactionServiceMock.getTransactionHistory.mock.calls[0] as [URLSearchParams];
        expect(params.get('name')).toBe('tej');
    });

    it('should reset the form, clear the query params and reload when clearInputs is called', () => {
        setup({ isHistoryMode: true, needSearchField: true });
        transactionServiceMock.getTransactionHistory.mockClear();
        routerMock.navigate.mockClear();

        const nameInput = fixture.nativeElement.querySelector('#name');
        nameInput.value = 'kávé';
        nameInput.dispatchEvent(new Event('input'));
        fixture.detectChanges();
        expect(component['filterForm'].value.name).toBe('kávé');

        const clearButton = fixture.nativeElement.querySelectorAll('form button')[1];
        clearButton.click();
        fixture.detectChanges();

        expect(component['filterForm'].value.name).toBe('');
        expect(routerMock.navigate).toHaveBeenCalledWith([], { queryParams: {} });
        expect(transactionServiceMock.getTransactionHistory).toHaveBeenCalledTimes(1);
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
        expect(fixture.nativeElement.querySelector('app-create-transaction-modal')).toBeTruthy();
    });
});
