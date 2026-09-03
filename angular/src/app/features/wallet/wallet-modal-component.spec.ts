import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';
import { WalletModalComponent } from './wallet-modal-component';
import { BaseModal } from '../../shared/components/modal/base-modal';
import { WalletDataInterface, WalletCreateRequest, WalletUpdateRequest } from './interfaces';
import { CurrencyCodesEnum, WalletTypesEnum } from '../../shared/enums';

@Component({
    selector: 'app-wallet-form-component',
    template: '',
})
class StubWalletFormComponent {
    @Input() wallet: WalletDataInterface | null = null;
    @Input() isFormDisabled = false;
    @Output() saved = new EventEmitter<WalletCreateRequest | WalletUpdateRequest>();
    @Output() deleted = new EventEmitter<number>();
}

describe('WalletModalComponent (Vitest)', () => {
    let fixture: ComponentFixture<WalletModalComponent>;
    let component: WalletModalComponent;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [WalletModalComponent],
            providers: [provideTranslateService()],
        }).compileComponents();

        TestBed.overrideComponent(WalletModalComponent, {
            set: { imports: [BaseModal, StubWalletFormComponent, TranslatePipe] },
        });

        fixture = TestBed.createComponent(WalletModalComponent);
        component = fixture.componentInstance;
        component.isFormDisabled = false;
    });

    it('should show a spinner instead of the form while data is initializing', () => {
        component.isDataInitializing = true;
        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('.spinner-border')).toBeTruthy();
        expect(fixture.nativeElement.querySelector('app-wallet-form-component')).toBeNull();
    });

    it('should render the form once data initializing is done', () => {
        component.isDataInitializing = false;
        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('.spinner-border')).toBeNull();
        expect(fixture.nativeElement.querySelector('app-wallet-form-component')).toBeTruthy();
    });

    it('should pass the wallet down to the form component', () => {
        const wallet: WalletDataInterface = {
            id: 3,
            name: 'Tárca',
            currencyCode: CurrencyCodesEnum.huf,
            type: WalletTypesEnum.savings,
            sum: 0,
        };
        component.wallet = wallet;
        component.isDataInitializing = false;
        fixture.detectChanges();

        const formDebugElement = fixture.debugElement.query(
            (de) => de.componentInstance instanceof StubWalletFormComponent,
        );
        expect((formDebugElement.componentInstance as StubWalletFormComponent).wallet).toEqual(
            wallet,
        );
    });

    it('should forward saved/deleted events from the form and closeModal from the base modal', () => {
        component.isDataInitializing = false;
        fixture.detectChanges();

        let savedPayload: unknown;
        component.saved.subscribe((value) => (savedPayload = value));
        let deletedId: number | undefined;
        component.deleted.subscribe((id) => (deletedId = id));
        let closed = false;
        component.closeModal.subscribe(() => (closed = true));

        const formDebugElement = fixture.debugElement.query(
            (de) => de.componentInstance instanceof StubWalletFormComponent,
        );
        const formStub = formDebugElement.componentInstance as StubWalletFormComponent;
        formStub.saved.emit({ name: 'x', walletType: WalletTypesEnum.default });
        formStub.deleted.emit(7);

        fixture.debugElement.query((de) => de.componentInstance instanceof BaseModal)
            .componentInstance.closeModal.emit();

        expect(savedPayload).toEqual({ name: 'x', walletType: WalletTypesEnum.default });
        expect(deletedId).toBe(7);
        expect(closed).toBe(true);
    });
});
