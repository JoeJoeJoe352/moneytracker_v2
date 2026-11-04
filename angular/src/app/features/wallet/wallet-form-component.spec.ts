import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideTranslateService } from '@ngx-translate/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { WalletFormComponent, WalletFormInputInterface } from './wallet-form-component';
import { WalletListDataInterface, WalletCreateRequest, WalletUpdateRequest } from './interfaces';
import {
    CurrencyCodesEnum,
    RecurrenceIntervalEnum,
    RecurringTransactionStatusEnum,
    TransactionTypeEnum,
    WalletTypesEnum,
} from '@shared/enums';
import { RecurringTransactionResponseInterface } from '../recurring-transactions/interfaces';

describe('WalletFormComponent (Vitest)', () => {
    let fixture: ComponentFixture<WalletFormComponent>;
    let component: WalletFormComponent;
    let routerMock: { navigate: ReturnType<typeof vi.fn> };

    async function setup(wallet: WalletListDataInterface | null) {
        routerMock = { navigate: vi.fn(() => Promise.resolve(true)) };

        await TestBed.configureTestingModule({
            imports: [WalletFormComponent],
            providers: [
                provideTranslateService(),
                {
                    provide: MAT_DIALOG_DATA,
                    useValue: { wallet, isFormDisabled: signal(false) } as WalletFormInputInterface,
                },
                { provide: Router, useValue: routerMock },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(WalletFormComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    }

    describe('create mode', () => {
        beforeEach(() => setup(null));

        it('should build an empty, invalid form by default with the create button enabled', () => {
            expect(component.name.value).toBe('');
            expect(component['walletForm'].valid).toBe(false);

            // a gomb érvénytelen formnál is aktív, hogy a kattintás után látszódjanak a hibák
            const submitButton = fixture.nativeElement.querySelector('button[type="submit"]');
            expect(submitButton.disabled).toBe(false);
        });

        it('should leave the currency select enabled', () => {
            expect(component['walletForm'].controls.currencyCode.disabled).toBe(false);
        });

        it('should not render a delete button', () => {
            expect(fixture.nativeElement.querySelector('.mat-button-danger')).toBeNull();
        });

        it('should not render the recurring transactions section', () => {
            expect(fixture.nativeElement.querySelector('.recurring-transactions')).toBeNull();
        });

        it('should not emit "saved" and should mark controls touched when submitting an invalid form', () => {
            let emitted = false;
            component.saved.subscribe(() => (emitted = true));

            component.onSubmit();

            expect(emitted).toBe(false);
            expect(component.name.touched).toBe(true);
        });

        it('should emit a WalletCreateRequest with name, currency and type on valid submit', () => {
            component.name.setValue('Megtakarítás');
            component['walletForm'].controls.currencyCode.setValue(CurrencyCodesEnum.eur);
            component['walletForm'].controls.walletType.setValue(WalletTypesEnum.savings);
            fixture.detectChanges();

            let emittedValue: WalletCreateRequest | WalletUpdateRequest | undefined;
            component.saved.subscribe((value) => (emittedValue = value));

            component.onSubmit();

            expect(emittedValue).toEqual({
                name: 'Megtakarítás',
                currencyCode: CurrencyCodesEnum.eur,
                walletType: WalletTypesEnum.savings,
            });
        });
    });

    describe('edit mode', () => {
        const existingWallet: WalletListDataInterface = {
            id: 9,
            name: 'Fő tárca',
            currencyCode: CurrencyCodesEnum.usd,
            type: WalletTypesEnum.default,
            sum: 0,
            lastSyncDate: '2026-09-15',
            recurringTransactions: [],
            deletable: true,
        };

        beforeEach(() => setup(existingWallet));

        it('should patch the form with the existing wallet data', () => {
            expect(component.name.value).toBe('Fő tárca');
            expect(component['walletForm'].controls.currencyCode.value).toBe(CurrencyCodesEnum.usd);
            expect(component['walletForm'].controls.walletType.value).toBe(WalletTypesEnum.default);
        });

        it('should disable the currency select, since it cannot change after creation', () => {
            expect(component['walletForm'].controls.currencyCode.disabled).toBe(true);
        });

        it('should emit a WalletUpdateRequest without currencyCode on valid submit', () => {
            component.name.setValue('Módosított név');
            fixture.detectChanges();

            let emittedValue: WalletCreateRequest | WalletUpdateRequest | undefined;
            component.saved.subscribe((value) => (emittedValue = value));

            component.onSubmit();

            expect(emittedValue).toEqual({
                name: 'Módosított név',
                walletType: WalletTypesEnum.default,
            });
        });

        it('should show a delete button and emit its id on click', () => {
            let deletedId: number | undefined;
            component.deleted.subscribe((id) => (deletedId = id));

            const deleteButton = fixture.nativeElement.querySelector('.mat-button-danger');
            expect(deleteButton).toBeTruthy();
            deleteButton.click();

            expect(deletedId).toBe(9);
        });
    });

    describe('not deletable', () => {
        const existingWallet: WalletListDataInterface = {
            id: 9,
            name: 'Fő tárca',
            currencyCode: CurrencyCodesEnum.usd,
            type: WalletTypesEnum.default,
            sum: 0,
            lastSyncDate: '2026-09-15',
            recurringTransactions: [],
            deletable: false,
        };

        beforeEach(() => setup(existingWallet));
        it('Must not show the delete button', () => {
            const deleteButton = fixture.nativeElement.querySelector('.mat-button-danger');
            expect(deleteButton).toBeFalsy();
        });
    });

    describe('recurring transactions', () => {
        const walletData = {
            id: 9,
            name: 'Fő tárca',
            currencyCode: CurrencyCodesEnum.huf,
            type: WalletTypesEnum.default,
        };
        const rent: RecurringTransactionResponseInterface = {
            id: 5,
            name: 'Albérlet',
            priceSum: -150000,
            startDate: '2026-01-01',
            nextOccurrenceDate: '2026-12-01',
            recurrenceInterval: RecurrenceIntervalEnum.MONTHLY,
            transactionType: TransactionTypeEnum.OUTCOME,
            status: RecurringTransactionStatusEnum.ACTIVE,
            categories: [],
            wallet: walletData,
        };
        const pocketMoney: RecurringTransactionResponseInterface = {
            ...rent,
            id: 6,
            name: 'Zsebpénz',
            priceSum: 5000,
            transactionType: TransactionTypeEnum.INCOME,
        };

        function walletWith(
            recurringTransactions: RecurringTransactionResponseInterface[],
        ): WalletListDataInterface {
            return {
                ...walletData,
                sum: 0,
                lastSyncDate: '2026-09-15',
                deletable: true,
                recurringTransactions,
            };
        }

        function rows(): HTMLElement[] {
            return Array.from(
                fixture.nativeElement.querySelectorAll('app-recurring-transaction-row-component'),
            );
        }

        it('should list the recurring transactions of the wallet in order, without the wallet name', async () => {
            await setup(walletWith([rent, pocketMoney]));

            expect(
                fixture.nativeElement.querySelector('.recurring-transactions h3').textContent,
            ).toContain('wallet.recurring_transactions');
            expect(rows().map((row) => row.querySelector('.name')?.textContent?.trim())).toEqual([
                'Albérlet',
                'Zsebpénz',
            ]);
            expect(fixture.nativeElement.querySelector('.wallet-name')).toBeNull();
        });

        it('should navigate to the recurring transaction when its row is selected', async () => {
            await setup(walletWith([rent, pocketMoney]));

            rows()[1]!.click();

            expect(routerMock.navigate).toHaveBeenCalledWith(['/recurring-transactions', 6]);
        });

        it('should not render the section when the wallet has no recurring transactions', async () => {
            await setup(walletWith([]));

            expect(fixture.nativeElement.querySelector('.recurring-transactions')).toBeNull();
        });
    });
});
