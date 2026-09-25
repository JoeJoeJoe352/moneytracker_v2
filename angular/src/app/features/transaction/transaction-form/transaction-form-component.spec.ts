import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideTranslateService } from '@ngx-translate/core';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatSelect } from '@angular/material/select';
import { TransactionFormComponent } from './transaction-form-component';
import { CategorySelectComponent } from './category-select-component';
import { TransactionService } from '../transaction-service';
import { TransactionUtils } from '../transaction-utils';
import { TransactionDataFromBackend } from '../interfaces';
import { CurrencyCodesEnum, TransactionTypeEnum, WalletTypesEnum } from '@shared/enums';
import { WalletDataInterface } from '../../wallet/interfaces';
import { UserDataStore } from '@app/shared/stores/user-data-store';

describe('TransactionFormComponent (Vitest)', () => {
    let fixture: ComponentFixture<TransactionFormComponent>;
    let component: TransactionFormComponent;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [TransactionFormComponent],
            providers: [
                provideTranslateService(),
                provideNativeDateAdapter(),
                { provide: TransactionService, useValue: { utils: new TransactionUtils() } },
                {
                    provide: UserDataStore,
                    useValue: {
                        getDefaultWallet: () => ({
                            id: 1,
                            name: 'Test Wallet',
                            type: WalletTypesEnum.default,
                            currencyCode: CurrencyCodesEnum.huf

                        }) as WalletDataInterface,
                        getWallets: () => [
                            {
                                id: 1,
                                name: 'Test Wallet',
                                type: WalletTypesEnum.default,
                                currencyCode: CurrencyCodesEnum.huf,
                            },
                            {
                                id: 2,
                                name: 'Euro Wallet',
                                type: WalletTypesEnum.default,
                                currencyCode: CurrencyCodesEnum.eur,
                            },
                        ] as WalletDataInterface[],
                    },
                },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(TransactionFormComponent);
        component = fixture.componentInstance;
        fixture.componentRef.setInput('isTransactionFormDisabled', false);
        fixture.componentRef.setInput('isCategorySaveInProgress', false);
        fixture.componentRef.setInput('categoryList', []);
        fixture.componentRef.setInput('addCategoryCallback', () => {
            throw new Error('not called in this test');
        });
    });

    it('should build an empty, invalid form for a new transaction by default', () => {
        fixture.detectChanges();

        expect(component.name.value).toBe('');
        expect(component.name.invalid).toBe(true);
        expect(component.isIncome.value).toBe(false);
        expect(component.isComplexTransaction.value).toBe(false);
        expect(component.details.length).toBe(0);

        // a mentés gomb érvénytelen formnál is aktív, hogy a kattintás után látszódjanak a hibák
        const submitButton = fixture.nativeElement.querySelector('button[type="submit"]');
        expect(submitButton.disabled).toBe(false);
    });

    it('should not emit "saved" and should mark all controls touched when submitting an invalid form', () => {
        fixture.detectChanges();

        let emitted = false;
        component.saved.subscribe(() => (emitted = true));

        fixture.nativeElement.querySelector('button[type="submit"]').click();

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

        submitButton.click();

        expect(emittedValue).toMatchObject({ name: 'Bevásárlás', price: 1000 });
    });

    it('should emit "saved" after toggling the complex mode on and back off (the empty detail row must not block the save)', () => {
        fixture.detectChanges();

        component.name.setValue('Bevásárlás');
        component.price.setValue(1000);
        component.transactionDate.setValue(new Date('2024-01-10'));

        const toggle = (): void => {
            fixture.nativeElement.querySelector('#transaction-show-details button').click();
            fixture.detectChanges();
        };
        toggle();
        expect(component.isComplexTransaction.value).toBe(true);
        expect(component.details.length).toBe(1);
        toggle();
        expect(component.isComplexTransaction.value).toBe(false);

        let emittedValue: unknown;
        component.saved.subscribe((value) => (emittedValue = value));
        fixture.nativeElement.querySelector('button[type="submit"]').click();

        // az üres detail sor egyszerű tranzakciónál nem validálódik
        expect(component.details.valid).toBe(true);
        expect(emittedValue).toMatchObject({ name: 'Bevásárlás', price: 1000 });
    });

    it('should keep the mode dependent validation after the form is disabled and re-enabled (e.g. during a save)', () => {
        fixture.detectChanges();

        component.name.setValue('Bevásárlás');
        component.transactionDate.setValue(new Date('2024-01-10'));
        fixture.nativeElement.querySelector('#transaction-show-details button').click();
        fixture.detectChanges();
        const detailRow = component.details.at(0);
        detailRow.controls.detailName.setValue('Kenyér');
        detailRow.controls.detailPrice.setValue(500);

        fixture.componentRef.setInput('isTransactionFormDisabled', true);
        fixture.detectChanges();
        fixture.componentRef.setInput('isTransactionFormDisabled', false);
        fixture.detectChanges();

        expect(component.price.enabled).toBe(true);
        expect(component.price.valid).toBe(true);
        expect(detailRow.controls.detailWeight.valid).toBe(true);

        let emittedValue: unknown;
        component.saved.subscribe((value) => (emittedValue = value));
        fixture.nativeElement.querySelector('button[type="submit"]').click();

        expect(emittedValue).toMatchObject({ isComplexTransaction: true });
    });

    it('should emit "saved" in complex mode without the hidden global price', () => {
        fixture.detectChanges();

        component.name.setValue('Bevásárlás');
        component.transactionDate.setValue(new Date('2024-01-10'));
        fixture.nativeElement.querySelector('#transaction-show-details button').click();
        fixture.detectChanges();

        const detailRow = component.details.at(0);
        detailRow.controls.detailName.setValue('Kenyér');
        detailRow.controls.detailPrice.setValue(500);

        let emittedValue: unknown;
        component.saved.subscribe((value) => (emittedValue = value));
        fixture.nativeElement.querySelector('button[type="submit"]').click();

        // az üres globális ár komplex tranzakciónál nem validálódik
        expect(component.price.valid).toBe(true);
        expect(emittedValue).toMatchObject({
            name: 'Bevásárlás',
            isComplexTransaction: true,
            details: [{ detailName: 'Kenyér', detailPrice: 500 }],
        });
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
        fixture.componentRef.setInput('transaction', backendTransaction);

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
        fixture.componentRef.setInput('transaction', null);

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
        expect(fixture.nativeElement.querySelector('.mat-button-danger')).toBeNull();

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
        fixture.componentRef.setInput('transaction', backendTransaction);
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

        const deleteButton = fixture.nativeElement.querySelector('.mat-button-danger');
        expect(deleteButton).toBeTruthy();
        deleteButton.click();

        expect(deletedId).toBe(42);
    });

    it('should add and remove detail rows, refusing to remove the last one', () => {
        fixture.detectChanges();

        // Közvetlenül hívjuk, mert az utolsó sor törlés gombja le van tiltva, így az ellenőrzés kattintással nem érhető el
        component['addRow']();
        component['addRow']();
        expect(component.details.length).toBe(2);

        component['deleteRow'](0);
        expect(component.details.length).toBe(1);

        component['deleteRow'](0);
        expect(component.details.length).toBe(1); // utolsó sor nem törölhető
    });

    it('should pass addCategoryCallback through to the category selects', () => {
        const addCategoryCallback = () => {
            throw new Error('not called in this test');
        };
        fixture.componentRef.setInput('addCategoryCallback', addCategoryCallback);
        fixture.detectChanges();

        const categorySelects = fixture.debugElement.queryAll(By.directive(CategorySelectComponent));
        expect(categorySelects.length).toBeGreaterThan(0);
        categorySelects.forEach((categorySelect) => {
            expect(
                (categorySelect.componentInstance as CategorySelectComponent).addCategoryCallback(),
            ).toBe(addCategoryCallback);
        });
    });

    it('should initialize the price suffix from the default wallet currency', () => {
        fixture.detectChanges();

        const priceSuffix = fixture.nativeElement.querySelector('[matTextSuffix]');
        expect(priceSuffix.textContent.trim()).toBe('Ft');
    });

    it('should update the price suffix when the wallet select changes', () => {
        fixture.detectChanges();

        component.walletId.setValue(2);
        fixture.detectChanges();

        const priceSuffix = fixture.nativeElement.querySelector('[matTextSuffix]');
        expect(priceSuffix.textContent.trim()).toBe('€');
    });

    it('should set selectedWalletsCurrency from the transaction wallet via ngOnChanges', () => {
        const backendTransaction: TransactionDataFromBackend = {
            id: 7,
            name: 'Fizetés',
            priceSum: 5000,
            transactionType: TransactionTypeEnum.INCOME,
            transactionDate: '2024-02-01',
            isComplexTransaction: false,
            walletId: 2,
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
        fixture.componentRef.setInput('transaction', backendTransaction);

        component.ngOnChanges({
            transaction: {
                currentValue: backendTransaction,
                previousValue: null,
                firstChange: true,
                isFirstChange: () => true,
            },
        });
        fixture.detectChanges();

        const priceSuffix = fixture.nativeElement.querySelector('[matTextSuffix]');
        expect(priceSuffix.textContent.trim()).toBe('€');
    });

    it('should render the wallet options with their currency code and the price suffix with the selected currency symbol', () => {
        fixture.detectChanges();

        const walletSelect = fixture.debugElement.query(By.directive(MatSelect))
            .componentInstance as MatSelect;
        const optionTexts = walletSelect.options.map((option) => option.viewValue.trim());
        expect(optionTexts).toEqual(['Test Wallet (HUF)', 'Euro Wallet (EUR)']);

        const priceSuffix = fixture.nativeElement.querySelector('[matTextSuffix]');
        expect(priceSuffix.textContent.trim()).toBe('Ft');
    });
});
