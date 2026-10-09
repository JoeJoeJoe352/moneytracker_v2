import { describe, it, expect } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal, WritableSignal } from '@angular/core';
import { provideTranslateService } from '@ngx-translate/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { WalletSyncFormComponent } from './wallet-sync-form-component';
import { WalletDataInterface, WalletSyncData, WalletSyncFormInputInterface } from './interfaces';
import { CurrencyCodesEnum, WalletTypesEnum } from '@shared/enums';

describe('WalletSyncFormComponent (Vitest)', () => {
    let fixture: ComponentFixture<WalletSyncFormComponent>;
    let component: WalletSyncFormComponent;
    let isLoading: WritableSignal<boolean>;

    const wallet: WalletDataInterface = {
        id: 7,
        name: 'Napi költés',
        currencyCode: CurrencyCodesEnum.huf,
        type: WalletTypesEnum.default,
        sum: 1000,
        lastSyncDate: '2026-09-15',
    };

    async function setup() {
        isLoading = signal(false);

        await TestBed.configureTestingModule({
            imports: [WalletSyncFormComponent],
            providers: [
                provideTranslateService(),
                {
                    provide: MAT_DIALOG_DATA,
                    useValue: { wallet, isLoading } as WalletSyncFormInputInterface,
                },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(WalletSyncFormComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    }

    function typeBalance(value: string): void {
        const input: HTMLInputElement = fixture.nativeElement.querySelector('#sync-currentBalance');
        input.value = value;
        input.dispatchEvent(new Event('input'));
        fixture.detectChanges();
    }

    function submitButton(): HTMLButtonElement {
        return fixture.nativeElement.querySelector('button[type="submit"]');
    }

    function collectEmits(): WalletSyncData[] {
        const emitted: WalletSyncData[] = [];
        component.syncFormSended.subscribe((data) => emitted.push(data));
        return emitted;
    }

    it('should emit the wallet id and the typed balance on submit', async () => {
        await setup();
        const emitted = collectEmits();

        typeBalance('1234.5');
        submitButton().click();

        expect(emitted).toEqual([{ walletId: 7, currentBalance: 1234.5 }]);
    });

    it('should accept 0 as a balance', async () => {
        await setup();
        const emitted = collectEmits();

        typeBalance('0');
        submitButton().click();

        expect(emitted).toEqual([{ walletId: 7, currentBalance: 0 }]);
    });

    it('should not emit and should show the required error when the balance is empty', async () => {
        await setup();
        const emitted = collectEmits();

        submitButton().click();
        fixture.detectChanges();

        expect(emitted).toEqual([]);
        expect(fixture.nativeElement.querySelector('mat-error').textContent).toContain(
            'wallet.sync.current_balance.required',
        );
    });

    it('should not emit and should show the min error when the balance is negative', async () => {
        await setup();
        const emitted = collectEmits();

        typeBalance('-1');
        submitButton().click();
        fixture.detectChanges();

        expect(emitted).toEqual([]);
        expect(fixture.nativeElement.querySelector('mat-error').textContent).toContain(
            'wallet.sync.current_balance.min',
        );
    });

    it('should disable the submit button while loading', async () => {
        await setup();
        expect(submitButton().disabled).toBe(false);

        isLoading.set(true);
        fixture.detectChanges();

        expect(submitButton().disabled).toBe(true);
    });
});
