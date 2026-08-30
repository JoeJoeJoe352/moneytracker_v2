import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { signal } from '@angular/core';
import { TransactionFormComponent } from './transaction-form-component';
import { TransactionService } from '../transaction/transaction-service';
import { TransactionUtils } from '../transaction/transaction-utils';
import { CategoryResponseInterface, TransactionDataFromBackend } from '../transaction/interfaces';
import { TransactionTypeEnum } from '../transaction/transaction-type-enum';
import { UserDataStore } from '../../shared/services/user-data-store';
import { WalletDataInterface } from '../auth/interfaces';
import { CurrencyCodes, WalletTypes } from '../../shared/enums';

describe('TransactionFormComponent (Vitest)', () => {
    let fixture: ComponentFixture<TransactionFormComponent>;
    let component: TransactionFormComponent;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [TransactionFormComponent],
            providers: [
                provideTranslateService(),
                { provide: TransactionService, useValue: { utils: new TransactionUtils() } },
                {
                    provide: UserDataStore,
                    useValue: {
                        getDefaultWallet: () => ({
                            id: 1,
                            name: 'Test Wallet',
                            type: WalletTypes.default,
                            currencyCode: CurrencyCodes.huf

                        }) as WalletDataInterface,
                    },
                },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(TransactionFormComponent);
        component = fixture.componentInstance;
        component.isTransactionFormDisabled = false;
        component.isCategorySaveInProgress = false;
        component.categoryList = signal<CategoryResponseInterface[]>([]);
    });

    it('should build an empty, invalid form for a new transaction by default', () => {
        fixture.detectChanges();

        expect(component.name.value).toBe('');
        expect(component.isIncome.value).toBe(false);
        expect(component.isComplexTransaction.value).toBe(false);
        expect(component.details.length).toBe(0);

        const submitButton = fixture.nativeElement.querySelector('button[type="submit"]');
        expect(submitButton.disabled).toBe(true);
    });

    it('should not emit "saved" and should mark all controls touched when submitting an invalid form', () => {
        fixture.detectChanges();

        let emitted = false;
        component.saved.subscribe(() => (emitted = true));

        component.onSubmit();

        expect(emitted).toBe(false);
        expect(component.name.touched).toBe(true);
    });

    it('should emit "saved" with the form value when submitting a valid form', () => {
        fixture.detectChanges();

        component.name.setValue('Bevásárlás');
        component.price.setValue(1000);
        component.transactionDate.setValue(new Date('2024-01-10'));
        fixture.detectChanges();

        const submitButton = fixture.nativeElement.querySelector('button[type="submit"]');
        expect(submitButton.disabled).toBe(false);

        let emittedValue: unknown;
        component.saved.subscribe((value) => (emittedValue = value));

        component.onSubmit();

        expect(emittedValue).toMatchObject({ name: 'Bevásárlás', price: 1000 });
    });

    it('should patch the form from an existing transaction via ngOnChanges', () => {
        const backendTransaction: TransactionDataFromBackend = {
            id: 7,
            name: 'Fizetés',
            priceSum: 5000,
            transactionType: TransactionTypeEnum.INCOME,
            transactionDate: '2024-02-01',
            isComplexTransaction: false,
            walletId: 1,
            transactionDetails: [
                {
                    name: 'sum',
                    price: 5000,
                    weight: null,
                    unitPrice: null,
                    isComplexPriceMode: false,
                    categories: [],
                },
            ],
        };
        component.transaction = backendTransaction;

        component.ngOnChanges({
            transaction: {
                currentValue: backendTransaction,
                previousValue: null,
                firstChange: true,
                isFirstChange: () => true,
            },
        });
        fixture.detectChanges();

        expect(component.name.value).toBe('Fizetés');
        expect(component.isIncome.value).toBe(true);
        expect(component.price.value).toBe(5000);
        expect(component.transactionDate.value).toEqual(new Date('2024-02-01'));
        expect(component.details.length).toBe(1);
    });

    it('should do nothing when ngOnChanges fires with a null transaction (initial run)', () => {
        component.transaction = null;

        expect(() =>
            component.ngOnChanges({
                transaction: {
                    currentValue: null,
                    previousValue: undefined,
                    firstChange: true,
                    isFirstChange: () => true,
                },
            }),
        ).not.toThrow();

        expect(component.details.length).toBe(0);
    });

    it('should show the delete button only for an existing transaction, and emit its id on click', () => {
        fixture.detectChanges();
        expect(fixture.nativeElement.querySelector('.btn-primary-red')).toBeNull();

        const backendTransaction: TransactionDataFromBackend = {
            id: 42,
            name: 'Fizetés',
            priceSum: 1000,
            transactionType: TransactionTypeEnum.INCOME,
            transactionDate: '2024-02-01',
            isComplexTransaction: false,
            walletId: 1,
            transactionDetails: [
                {
                    name: 'sum',
                    price: 1000,
                    weight: null,
                    unitPrice: null,
                    isComplexPriceMode: false,
                    categories: [],
                },
            ],
        };
        component.transaction = backendTransaction;
        component.ngOnChanges({
            transaction: {
                currentValue: backendTransaction,
                previousValue: null,
                firstChange: false,
                isFirstChange: () => false,
            },
        });
        fixture.detectChanges();

        let deletedId: number | undefined;
        component.deleted.subscribe((id) => (deletedId = id));

        const deleteButton = fixture.nativeElement.querySelector('.btn-primary-red');
        expect(deleteButton).toBeTruthy();
        deleteButton.click();

        expect(deletedId).toBe(42);
    });

    it('should add and remove detail rows, refusing to remove the last one', () => {
        fixture.detectChanges();

        component.addRow();
        component.addRow();
        expect(component.details.length).toBe(2);

        component.deleteRow(0);
        expect(component.details.length).toBe(1);

        component.deleteRow(0);
        expect(component.details.length).toBe(1); // utolsó sor nem törölhető
    });

    it('should emit categoryAdded only when the "no filtered data" button is clicked with a non-empty search text', () => {
        fixture.detectChanges();

        let addedCategory: string | undefined;
        component.categoryAdded.subscribe((name) => (addedCategory = name));

        const otherTarget = document.createElement('div');
        component.onCategoryDropdownClick({ target: otherTarget } as unknown as Event);
        expect(addedCategory).toBeUndefined();

        const noFilteredDataButton = document.createElement('div');
        noFilteredDataButton.classList.add('no-filtered-data');
        component.onCategoryFilterChange('Új kategória');
        component.onCategoryDropdownClick({ target: noFilteredDataButton } as unknown as Event);

        expect(addedCategory).toBe('Új kategória');
    });

    it('should not emit categoryAdded when a category save is already in progress', () => {
        fixture.detectChanges();
        component.isCategorySaveInProgress = true;

        let addedCategory: string | undefined;
        component.categoryAdded.subscribe((name) => (addedCategory = name));

        component.onCategoryFilterChange('Új kategória');
        const noFilteredDataButton = document.createElement('div');
        noFilteredDataButton.classList.add('no-filtered-data');
        component.onCategoryDropdownClick({ target: noFilteredDataButton } as unknown as Event);

        expect(addedCategory).toBeUndefined();
    });
});
