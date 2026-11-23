import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideTranslateService } from '@ngx-translate/core';
import { provideNativeDateAdapter } from '@angular/material/core';
import { TransferTransactionFormComponent } from './transfer-transaction-form-component';
import { TransferTransactionFormValue } from './interfaces';
import { CurrencyCodesEnum, WalletTypesEnum } from '@shared/enums';
import { WalletDataInterface } from '../../wallet/interfaces';
import { UserDataStore } from '@app/shared/stores/user-data-store';

const wallets: WalletDataInterface[] = [
    {
        id: 1,
        name: 'Napi költés',
        type: WalletTypesEnum.default,
        currencyCode: CurrencyCodesEnum.huf,
    },
    {
        id: 2,
        name: 'Megtakarítás',
        type: WalletTypesEnum.default,
        currencyCode: CurrencyCodesEnum.huf,
    },
    { id: 3, name: 'Euró', type: WalletTypesEnum.default, currencyCode: CurrencyCodesEnum.eur },
];

describe('TransferTransactionFormComponent (Vitest)', () => {
    let fixture: ComponentFixture<TransferTransactionFormComponent>;
    let component: TransferTransactionFormComponent;
    let emitted: TransferTransactionFormValue[];

    beforeEach(async () => {
        TestBed.configureTestingModule({
            imports: [TransferTransactionFormComponent],
            providers: [
                provideTranslateService(),
                provideNativeDateAdapter(),
                {
                    provide: UserDataStore,
                    useValue: {
                        getDefaultWallet: () => wallets[0],
                        getWallets: () => wallets,
                    },
                },
            ],
        });
        await TestBed.compileComponents();

        fixture = TestBed.createComponent(TransferTransactionFormComponent);
        component = fixture.componentInstance;
        fixture.componentRef.setInput('isFormDisabled', false);
        emitted = [];
        component.saved.subscribe((value) => emitted.push(value));
        fixture.detectChanges();
    });

    /**
     * Kitölti a formot érvényes adatokkal (1-es walletből a 2-esbe)
     */
    function fillValidForm(): void {
        component.srcTransactionName.setValue('Megtakarításba');
        component.dstTransactionName.setValue('Napi költésből');
        component.dstWalletId.setValue(2);
        component.priceSum.setValue(5000);
        component.transactionDate.setValue(new Date(2026, 10, 20));
    }

    function submit(): void {
        fixture.debugElement.query(By.css('form')).triggerEventHandler('submit', {});
        fixture.detectChanges();
    }

    function walletErrorText(): string {
        const element = fixture.debugElement.query(By.css('.wallet-error'))
            .nativeElement as HTMLElement;
        return element.textContent?.trim() ?? '';
    }

    it('should start with the default wallet as source and an empty, invalid form', () => {
        expect(component.srcWalletId.value).toBe(1);
        expect(component.dstWalletId.value).toBeNull();
        expect(component.srcTransactionName.invalid).toBe(true);
        expect(component.priceSum.invalid).toBe(true);
    });

    it('should emit the form value on submit when the form is valid', () => {
        fillValidForm();

        submit();

        expect(emitted).toEqual([
            {
                srcTransactionName: 'Megtakarításba',
                srcWalletId: 1,
                dstTransactionName: 'Napi költésből',
                dstWalletId: 2,
                transactionDate: new Date(2026, 10, 20),
                priceSum: 5000,
            },
        ]);
    });

    it('should not emit and mark the fields as touched when the form is invalid', () => {
        submit();

        expect(emitted).toEqual([]);
        expect(component.srcTransactionName.touched).toBe(true);
        expect(component.dstWalletId.touched).toBe(true);
    });

    it('should not emit when the source and destination wallet are the same', () => {
        fillValidForm();
        component.dstWalletId.setValue(1);

        submit();

        expect(emitted).toEqual([]);
        expect(walletErrorText()).toBe('transfer_transaction.same_wallet');
    });

    it('should not emit when the wallets have different currencies', () => {
        fillValidForm();
        component.dstWalletId.setValue(3);

        submit();

        expect(emitted).toEqual([]);
        expect(walletErrorText()).toBe('transfer_transaction.different_currency');
    });

    it('should not show a wallet error before the user touched the wallet fields', () => {
        component.dstWalletId.setValue(1);
        fixture.detectChanges();

        expect(walletErrorText()).toBe('');
    });

    it('should reject a non-positive amount', () => {
        fillValidForm();
        component.priceSum.setValue(0);

        submit();

        expect(emitted).toEqual([]);
        expect(component.priceSum.hasError('greaterThan')).toBe(true);
    });

    it('should disable and re-enable the form based on the isFormDisabled input', () => {
        fixture.componentRef.setInput('isFormDisabled', true);
        fixture.detectChanges();
        expect(component.srcTransactionName.disabled).toBe(true);

        fixture.componentRef.setInput('isFormDisabled', false);
        fixture.detectChanges();
        expect(component.srcTransactionName.enabled).toBe(true);
    });
});
