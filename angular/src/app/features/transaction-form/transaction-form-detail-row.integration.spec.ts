import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideTranslateService } from '@ngx-translate/core';
import { signal } from '@angular/core';
import { TransactionFormComponent } from './transaction-form-component';
import { TransactionDetailRowComponent } from './transaction-detail-row-component';
import { TransactionService } from '../transaction/transaction-service';
import { TransactionUtils } from '../transaction/transaction-utils';
import { CategoryResponseInterface, TransactionDataFromBackend } from '../transaction/interfaces';
import { TransactionTypeEnum } from '../transaction/transaction-type-enum';
import { UserDataStore } from '../../shared/services/user-data-store';
import { WalletDataInterface } from '../wallet/interfaces';
import { CurrencyCodesEnum, WalletTypesEnum } from '../../shared/enums';

describe('TransactionForm + TransactionDetailRow integration (Vitest)', () => {
    let fixture: ComponentFixture<TransactionFormComponent>;
    let component: TransactionFormComponent;

    const complexTransaction: TransactionDataFromBackend = {
        id: 9,
        name: 'Bevásárlás',
        priceSum: -2500,
        transactionType: TransactionTypeEnum.OUTCOME,
        transactionDate: '2024-03-01',
        isComplexTransaction: true,
        walletId: 1,
        transactionDetails: [
            {
                name: 'Kenyér',
                price: -1000,
                weight: null,
                unitPrice: null,
                isComplexPriceMode: false,
                categories: [],
            },
            {
                name: 'Tej',
                price: -1500,
                weight: null,
                unitPrice: null,
                isComplexPriceMode: false,
                categories: [],
            },
        ],
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [TransactionFormComponent],
            providers: [
                provideTranslateService(),
                { provide: TransactionService, useValue: { utils: new TransactionUtils() } },
                {
                    provide: UserDataStore,
                    useValue: {
                        getDefaultWallet: () =>
                            ({
                                id: 1,
                                name: 'Test Wallet',
                                type: WalletTypesEnum.default,
                                currencyCode: CurrencyCodesEnum.huf,
                            }) as WalletDataInterface,
                        getWallets: () =>
                            [
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
        component.isTransactionFormDisabled = false;
        component.isCategorySaveInProgress = false;
        component.categoryList = signal<CategoryResponseInterface[]>([]);
        component.transaction = complexTransaction;

        component.ngOnChanges({
            transaction: {
                currentValue: complexTransaction,
                previousValue: null,
                firstChange: true,
                isFirstChange: () => true,
            },
        });
        fixture.detectChanges();
    });

    it('should render one real detail row component per detail form group, correctly bound', () => {
        const rows = fixture.debugElement.queryAll(By.directive(TransactionDetailRowComponent));

        expect(rows.length).toBe(2);
        expect((rows[0].componentInstance as TransactionDetailRowComponent).detail).toBe(
            component.details.at(0),
        );
        expect((rows[1].componentInstance as TransactionDetailRowComponent).detail).toBe(
            component.details.at(1),
        );
        expect(fixture.nativeElement.querySelector('#detail-name-0').value).toBe('Kenyér');
        expect(fixture.nativeElement.querySelector('#detail-name-1').value).toBe('Tej');
    });

    it('should let the user add a new detail row via the "add" button', () => {
        const addButton = fixture.nativeElement.querySelector('.detail-add-row button');
        addButton.click();
        fixture.detectChanges();

        expect(
            fixture.nativeElement.querySelectorAll('app-transaction-detail-row-component').length,
        ).toBe(3);
        expect(component.details.length).toBe(3);
    });

    it('should remove a detail row when its own delete button is clicked', () => {
        const deleteButtons = fixture.nativeElement.querySelectorAll(
            'app-transaction-detail-row-component .btn-primary-red',
        );
        expect(deleteButtons.length).toBe(2);

        deleteButtons[0].click();
        fixture.detectChanges();

        expect(
            fixture.nativeElement.querySelectorAll('app-transaction-detail-row-component').length,
        ).toBe(1);
        expect(fixture.nativeElement.querySelector('#detail-name-0').value).toBe('Tej');
    });

    it("should disable every row's delete button once only one detail row remains", () => {
        const deleteButtons = fixture.nativeElement.querySelectorAll(
            'app-transaction-detail-row-component .btn-primary-red',
        );
        deleteButtons[0].click();
        fixture.detectChanges();

        const remainingDeleteButton = fixture.nativeElement.querySelector(
            'app-transaction-detail-row-component .btn-primary-red',
        );
        expect(remainingDeleteButton.disabled).toBe(true);
    });

    it('should switch a detail row between simple and complex price mode via its own switch input', () => {
        expect(fixture.nativeElement.querySelector('#detail-price-0')).toBeTruthy();
        expect(fixture.nativeElement.querySelector('#detail-weight-0')).toBeNull();

        const rowDetailGroup = component.details.at(0);
        rowDetailGroup.controls.detailIsComplexPriceMode.setValue(true);
        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('#detail-price-0')).toBeNull();
        expect(fixture.nativeElement.querySelector('#detail-weight-0')).toBeTruthy();
        expect(fixture.nativeElement.querySelector('#detail-unitprice-0')).toBeTruthy();

        // a szimpla price control letiltódik, amíg komplex módban vagyunk
        expect(rowDetailGroup.controls.detailPrice.disabled).toBe(true);
        expect(rowDetailGroup.controls.detailWeight.disabled).toBe(false);
        expect(rowDetailGroup.controls.detailUnitPrice.disabled).toBe(false);

        // a másik sor nem érintett
        expect(component.details.at(1).controls.detailPrice.disabled).toBe(false);
    });

    it('should forward category-related outputs from a detail row up to the form component, driving categoryAdded', () => {
        let addedCategory: string | undefined;
        component.categoryAdded.subscribe((name) => (addedCategory = name));

        const firstDropdown = fixture.debugElement.query(By.css('ng-multiselect-dropdown'));
        firstDropdown.triggerEventHandler('onFilterChange', 'tej');

        const noFilteredDataButton = document.createElement('div');
        noFilteredDataButton.classList.add('no-filtered-data');
        firstDropdown.triggerEventHandler('click', { target: noFilteredDataButton });

        expect(addedCategory).toBe('tej');
    });

    it('should propagate the selected wallet currency symbol down to every detail row', () => {
        const rows = fixture.debugElement.queryAll(By.directive(TransactionDetailRowComponent));

        expect(
            rows.map((row) => (row.componentInstance as TransactionDetailRowComponent).currencySymbol),
        ).toEqual(['Ft', 'Ft']);

        const walletSelect = fixture.nativeElement.querySelector('#transaction-wallet');
        walletSelect.value = '2';
        walletSelect.dispatchEvent(new Event('change'));
        fixture.detectChanges();

        const suffixes = fixture.nativeElement.querySelectorAll(
            'app-transaction-detail-row-component .suffix',
        );
        expect(suffixes[0].textContent.trim()).toBe('€');
    });
});
