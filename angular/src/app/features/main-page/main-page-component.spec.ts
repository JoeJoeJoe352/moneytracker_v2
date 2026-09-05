import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Subject, of } from 'rxjs';
import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';
import { MainPage } from './main-page-component';
import { TransactionService } from '../transaction/transaction-service';
import { CategoryService } from '../transaction/category-service';
import { MoneySumInterface } from '../transaction/interfaces';
import { TransactionModalStateService } from '../transaction/transaction-modal-state-service';
import { CurrencyCodesEnum } from '../../shared/enums';
import { CurrencyFormatPipe } from '../../shared/pipes/currency-format-pipe';

@Component({
    selector: 'app-transactions-list-component',
    template: '',
})
class StubTransactionsListComponent {
    @Input() isHistoryMode = false;
    @Input() needSearchField = false;
    @Input() reloadTrigger = 0;
    @Output() transactionsChanged = new EventEmitter<void>();
}

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

describe('MainPage (Vitest)', () => {
    let fixture: ComponentFixture<MainPage>;
    let getMoneySum$: Subject<MoneySumInterface>;
    let getMoneySumSpy: ReturnType<typeof vi.fn>;

    function setup() {
        getMoneySum$ = new Subject<MoneySumInterface>();
        getMoneySumSpy = vi.fn(() => getMoneySum$.asObservable());

        TestBed.configureTestingModule({
            imports: [MainPage],
            providers: [
                provideTranslateService(),
                {
                    provide: TransactionService,
                    useValue: {
                        getMoneySum: getMoneySumSpy,
                        getLastTransactions: () => of([]),
                        getTransactionHistory: () => of([]),
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
            ],
        });
        TestBed.overrideComponent(MainPage, {
            set: {
                imports: [
                    StubTransactionsListComponent,
                    StubTransactionModalComponent,
                    TranslatePipe,
                    CurrencyFormatPipe,
                ],
            },
        });

        fixture = TestBed.createComponent(MainPage);
        fixture.detectChanges();
    }

    beforeEach(() => setup());

    it('should show a spinner while the money sum is loading, then render the totals once loaded', () => {
        expect(fixture.nativeElement.querySelector('.balance-card .spinner-border')).toBeTruthy();
        expect(fixture.nativeElement.querySelector('.balance-card .stat-value')).toBeNull();

        getMoneySum$.next({
            moneySum: [{ currencyCode: CurrencyCodesEnum.huf, total: 15000 }],
            incomeSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 5000 }],
            expenseSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 2000 }],
        });
        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('.balance-card .spinner-border')).toBeNull();
        expect(fixture.nativeElement.querySelector('.balance-card .stat-value').textContent).toContain(
            '15,000',
        );
        expect(fixture.nativeElement.querySelector('.stat-value.income').textContent).toContain('5,000');
        expect(fixture.nativeElement.querySelector('.stat-value.expense').textContent).toContain(
            '2,000',
        );
    });

    it('should render the balance trend as positive when income exceeds expense this month', () => {
        getMoneySum$.next({
            moneySum: [{ currencyCode: CurrencyCodesEnum.huf, total: 3000 }],
            incomeSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 5000 }],
            expenseSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 2000 }],
        });
        fixture.detectChanges();

        const trend = fixture.nativeElement.querySelector('.stat-trend');
        expect(trend.classList.contains('positive')).toBe(true);
        expect(trend.classList.contains('negative')).toBe(false);
        expect(trend.querySelector('.bi-arrow-up-short')).toBeTruthy();
        expect(trend.textContent).toContain('3,000');
    });

    it('should render the balance trend as negative when expense exceeds income this month', () => {
        getMoneySum$.next({
            moneySum: [{ currencyCode: CurrencyCodesEnum.huf, total: -1000 }],
            incomeSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 1000 }],
            expenseSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 2000 }],
        });
        fixture.detectChanges();

        const trend = fixture.nativeElement.querySelector('.stat-trend');
        expect(trend.classList.contains('negative')).toBe(true);
        expect(trend.classList.contains('positive')).toBe(false);
        expect(trend.querySelector('.bi-arrow-down-short')).toBeTruthy();
    });

    it('should open the transaction modal when the create-transaction button is clicked', () => {
        getMoneySum$.next({
            moneySum: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
            incomeSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
            expenseSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
        });
        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('app-create-transaction-modal')).toBeNull();

        const createButton = fixture.nativeElement.querySelector('.balance-card button');
        createButton.click();
        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('app-create-transaction-modal')).toBeTruthy();
    });

    it('should refetch only the money sum when the transaction list reports a change', () => {
        getMoneySum$.next({
            moneySum: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
            incomeSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
            expenseSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
        });
        fixture.detectChanges();
        expect(getMoneySumSpy).toHaveBeenCalledTimes(1);

        const listStub = fixture.debugElement.query(
            (de) => de.componentInstance instanceof StubTransactionsListComponent,
        ).componentInstance as StubTransactionsListComponent;
        listStub.transactionsChanged.emit();
        fixture.detectChanges();

        expect(getMoneySumSpy).toHaveBeenCalledTimes(2);
        expect(listStub.reloadTrigger).toBe(0); // csak az összesítés töltődik újra, a lista nem
    });

    it('should refetch both the money sum and bump the list reload trigger when the modal reports a save/delete', () => {
        getMoneySum$.next({
            moneySum: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
            incomeSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
            expenseSumThisMonth: [{ currencyCode: CurrencyCodesEnum.huf, total: 0 }],
        });
        fixture.detectChanges();
        expect(getMoneySumSpy).toHaveBeenCalledTimes(1);

        const listStub = fixture.debugElement.query(
            (de) => de.componentInstance instanceof StubTransactionsListComponent,
        ).componentInstance as StubTransactionsListComponent;
        expect(listStub.reloadTrigger).toBe(0);

        const modalState = fixture.debugElement.injector.get(TransactionModalStateService);
        modalState.changed.next();
        fixture.detectChanges();

        expect(getMoneySumSpy).toHaveBeenCalledTimes(2);
        expect(listStub.reloadTrigger).toBe(1);
    });
});
