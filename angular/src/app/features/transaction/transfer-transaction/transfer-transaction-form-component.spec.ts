import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideTranslateService } from '@ngx-translate/core';
import { provideNativeDateAdapter } from '@angular/material/core';
import { TransferTransactionFormComponent } from './transfer-transaction-form-component';
import { TransferTransactionFormValue, TransferTransactionResponse } from './interfaces';
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

const existingTransfer: TransferTransactionResponse = {
    transferId: 7,
    srcTransactionName: 'Megtakarításba',
    srcWalletId: 1,
    dstTransactionName: 'Napi költésből',
    dstWalletId: 2,
    transactionDate: '2026-11-01',
    priceSum: 1000,
};

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

    /**
     * Lenyitja a cél wallet dropdownt, és visszaadja az opciók szövegét
     */
    function dstWalletOptionTexts(): string[] {
        const trigger = fixture.nativeElement.querySelector(
            '#transfer-dst-wallet .mat-mdc-select-trigger',
        ) as HTMLElement;
        trigger.click();
        fixture.detectChanges();
        return Array.from(document.querySelectorAll('mat-option')).map(
            (option) => option.textContent?.trim() ?? '',
        );
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
    });

    it('should not emit when the wallets have different currencies', () => {
        fillValidForm();
        component.dstWalletId.setValue(3);

        submit();

        expect(emitted).toEqual([]);
    });

    it('should clear the destination wallet when the source is switched to the same wallet', () => {
        fillValidForm();

        component.srcWalletId.setValue(2);

        expect(component.dstWalletId.value).toBeNull();
    });

    it('should clear the destination wallet when the source is switched to another currency', () => {
        fillValidForm();

        component.srcWalletId.setValue(3);

        expect(component.dstWalletId.value).toBeNull();
    });

    it('should keep the destination wallet when it is still valid for the new source', () => {
        component.srcWalletId.setValue(2);
        component.dstWalletId.setValue(1);

        component.srcWalletId.setValue(2);

        expect(component.dstWalletId.value).toBe(1);
    });

    it('should offer only the other wallets with the same currency as destination', () => {
        expect(dstWalletOptionTexts()).toEqual(['Megtakarítás (HUF)']);
    });

    it('should show the no destination wallet option when no wallet has the same currency', () => {
        component.srcWalletId.setValue(3);
        fixture.detectChanges();

        expect(dstWalletOptionTexts()).toEqual(['transfer_transaction.no_dst_wallet']);
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

    it('should not show the delete button when creating a new transfer', () => {
        expect(fixture.nativeElement.querySelector('.mat-button-danger')).toBeNull();
    });

    it('should prefill the form and lock both wallets for an existing transfer', () => {
        fixture.componentRef.setInput('transfer', existingTransfer);
        fixture.detectChanges();

        expect(component.srcTransactionName.value).toBe('Megtakarításba');
        expect(component.srcWalletId.value).toBe(1);
        expect(component.dstTransactionName.value).toBe('Napi költésből');
        expect(component.dstWalletId.value).toBe(2);
        expect(component.transactionDate.value).toEqual(new Date(2026, 10, 1));
        expect(component.priceSum.value).toBe(1000);
        expect(component.srcWalletId.disabled).toBe(true);
        expect(component.dstWalletId.disabled).toBe(true);
        expect(component.srcTransactionName.enabled).toBe(true);
    });

    it('should keep the wallets locked for an existing transfer after the form is re-enabled', () => {
        fixture.componentRef.setInput('transfer', existingTransfer);
        fixture.detectChanges();

        fixture.componentRef.setInput('isFormDisabled', true);
        fixture.detectChanges();
        fixture.componentRef.setInput('isFormDisabled', false);
        fixture.detectChanges();

        expect(component.srcTransactionName.enabled).toBe(true);
        expect(component.srcWalletId.disabled).toBe(true);
        expect(component.dstWalletId.disabled).toBe(true);
    });

    it('should emit the edited values including the locked wallets on submit', () => {
        fixture.componentRef.setInput('transfer', existingTransfer);
        fixture.detectChanges();
        component.priceSum.setValue(2500);

        submit();

        expect(emitted).toEqual([
            {
                srcTransactionName: 'Megtakarításba',
                srcWalletId: 1,
                dstTransactionName: 'Napi költésből',
                dstWalletId: 2,
                transactionDate: new Date(2026, 10, 1),
                priceSum: 2500,
            },
        ]);
    });

    it('should emit deleted when the delete button of an existing transfer is clicked', () => {
        fixture.componentRef.setInput('transfer', existingTransfer);
        fixture.detectChanges();
        let deletedCount = 0;
        component.deleted.subscribe(() => deletedCount++);

        (fixture.nativeElement.querySelector('.mat-button-danger') as HTMLButtonElement).click();

        expect(deletedCount).toBe(1);
    });
});
