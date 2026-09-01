import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { WalletFormComponent } from './wallet-form-component';
import { WalletDataInterface, WalletCreateRequest, WalletUpdateRequest } from './interfaces';
import { CurrencyCodesEnum, WalletTypesEnum } from '../../shared/enums';

describe('WalletFormComponent (Vitest)', () => {
    let fixture: ComponentFixture<WalletFormComponent>;
    let component: WalletFormComponent;

    async function setup(wallet: WalletDataInterface | null) {
        await TestBed.configureTestingModule({
            imports: [WalletFormComponent],
            providers: [provideTranslateService()],
        }).compileComponents();

        fixture = TestBed.createComponent(WalletFormComponent);
        component = fixture.componentInstance;
        component.wallet = wallet;
        component.isFormDisabled = false;
        fixture.detectChanges();
    }

    describe('create mode', () => {
        beforeEach(() => setup(null));

        it('should build an empty, invalid form by default with the create button disabled', () => {
            expect(component.name.value).toBe('');
            expect(component['walletForm'].valid).toBe(false);

            const submitButton = fixture.nativeElement.querySelector('button[type="submit"]');
            expect(submitButton.disabled).toBe(true);
        });

        it('should leave the currency select enabled', () => {
            expect(component['walletForm'].controls.currencyCode.disabled).toBe(false);
        });

        it('should not render a delete button', () => {
            expect(fixture.nativeElement.querySelector('.btn-primary-red')).toBeNull();
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
        const existingWallet: WalletDataInterface = {
            id: 9,
            name: 'Fő tárca',
            currencyCode: CurrencyCodesEnum.usd,
            type: WalletTypesEnum.default,
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

            const deleteButton = fixture.nativeElement.querySelector('.btn-primary-red');
            expect(deleteButton).toBeTruthy();
            deleteButton.click();

            expect(deletedId).toBe(9);
        });
    });
});
