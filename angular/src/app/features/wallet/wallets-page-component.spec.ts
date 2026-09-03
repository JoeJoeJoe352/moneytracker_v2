import { describe, it, expect, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';
import { WalletsPageComponent } from './wallets-page-component';
import { WalletService } from './wallet-service';
import { WalletDataInterface, WalletCreateRequest, WalletUpdateRequest } from './interfaces';
import { CurrencyCodesEnum, WalletTypesEnum } from '../../shared/enums';

@Component({
    selector: 'app-wallets-list-component',
    template: '',
})
class StubWalletsListComponent {
    @Input() walletListData: WalletDataInterface[] = [];
    @Output() walletCardClicked = new EventEmitter<WalletDataInterface>();
}

@Component({
    selector: 'app-wallet-modal-component',
    template: '',
})
class StubWalletModalComponent {
    @Input() wallet: WalletDataInterface | null = null;
    @Input() isFormDisabled = false;
    @Input() isDataInitializing = false;
    @Output() closeModal = new EventEmitter<void>();
    @Output() saved = new EventEmitter<WalletCreateRequest | WalletUpdateRequest>();
    @Output() deleted = new EventEmitter<number>();
}

const sampleWallet: WalletDataInterface = {
    id: 1,
    name: 'Napi költés',
    currencyCode: CurrencyCodesEnum.huf,
    type: WalletTypesEnum.default,
    sum: 0,
};

describe('WalletsPageComponent (Vitest)', () => {
    let fixture: ComponentFixture<WalletsPageComponent>;
    let component: WalletsPageComponent;
    let walletServiceMock: {
        listWallets: ReturnType<typeof vi.fn>;
        createWallet: ReturnType<typeof vi.fn>;
        updateWallet: ReturnType<typeof vi.fn>;
        softDeleteWallet: ReturnType<typeof vi.fn>;
    };

    function setup(
        listWalletsResult: Observable<WalletDataInterface[]> = of([sampleWallet]),
    ) {
        walletServiceMock = {
            listWallets: vi.fn(() => listWalletsResult),
            createWallet: vi.fn(() => of(undefined)),
            updateWallet: vi.fn(() => of(undefined)),
            softDeleteWallet: vi.fn(() => of(undefined)),
        };

        TestBed.configureTestingModule({
            imports: [WalletsPageComponent],
            providers: [
                provideTranslateService(),
                { provide: WalletService, useValue: walletServiceMock },
            ],
        });
        TestBed.overrideComponent(WalletsPageComponent, {
            set: {
                imports: [StubWalletsListComponent, StubWalletModalComponent, TranslatePipe],
            },
        });

        fixture = TestBed.createComponent(WalletsPageComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    }

    function getListStub(): StubWalletsListComponent {
        return fixture.debugElement.query(
            (de) => de.componentInstance instanceof StubWalletsListComponent,
        ).componentInstance as StubWalletsListComponent;
    }

    function getModalStub(): StubWalletModalComponent {
        return fixture.debugElement.query(
            (de) => de.componentInstance instanceof StubWalletModalComponent,
        ).componentInstance as StubWalletModalComponent;
    }

    it('should load the wallet list on init', () => {
        setup();

        expect(walletServiceMock.listWallets).toHaveBeenCalledTimes(1);
        expect(getListStub().walletListData).toEqual([sampleWallet]);
    });

    // A jelenlegi implementáció a tap(() => isWalletListLoading.set(false))-t csak a sikeres
    // ágon futtatja le, hiba esetén nincs catchError, ezért a spinner örökre fennmarad.
    // Ez a teszt a jelenlegi (hibás) viselkedést rögzíti - lásd a kapcsolódó megjegyzést.
    it('should keep showing the spinner forever if loading the wallets fails (known bug: no catchError)', () => {
        setup(throwError(() => new Error('boom')));

        expect(component['isWalletListLoading']()).toBe(true);
        expect(fixture.nativeElement.querySelector('.spinner-border')).toBeTruthy();
        expect(fixture.nativeElement.querySelector('app-wallets-list-component')).toBeNull();
    });

    it('should not show the modal until it is opened', () => {
        setup();

        expect(fixture.nativeElement.querySelector('app-wallet-modal-component')).toBeNull();
    });

    it('should open the modal in create mode (no wallet) when the create button is clicked', () => {
        setup();

        const createButton = fixture.nativeElement.querySelector('button.btn-primary-green');
        createButton.click();
        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('app-wallet-modal-component')).toBeTruthy();
        expect(getModalStub().wallet).toBeNull();
    });

    it('should open the modal in edit mode with the clicked wallet when a card is clicked', () => {
        setup();

        getListStub().walletCardClicked.emit(sampleWallet);
        fixture.detectChanges();

        expect(getModalStub().wallet).toEqual(sampleWallet);
    });

    it('should create a new wallet and reload the list, then close the modal', () => {
        setup();
        walletServiceMock.listWallets.mockClear();

        component['openWalletModal'](null);
        fixture.detectChanges();

        const payload: WalletCreateRequest = {
            name: 'Új tárca',
            currencyCode: CurrencyCodesEnum.huf,
            walletType: WalletTypesEnum.default,
        };
        getModalStub().saved.emit(payload);
        TestBed.tick();

        expect(walletServiceMock.createWallet).toHaveBeenCalledWith(payload);
        expect(walletServiceMock.updateWallet).not.toHaveBeenCalled();
        expect(walletServiceMock.listWallets).toHaveBeenCalledTimes(1);
        fixture.detectChanges();
        expect(fixture.nativeElement.querySelector('app-wallet-modal-component')).toBeNull();
    });

    it('should update the selected wallet and reload the list, then close the modal', () => {
        setup();
        walletServiceMock.listWallets.mockClear();

        component['openWalletModal'](sampleWallet);
        fixture.detectChanges();

        const payload: WalletUpdateRequest = { name: 'Módosított', walletType: WalletTypesEnum.savings };
        getModalStub().saved.emit(payload);
        TestBed.tick();

        expect(walletServiceMock.updateWallet).toHaveBeenCalledWith(sampleWallet.id, payload);
        expect(walletServiceMock.createWallet).not.toHaveBeenCalled();
        expect(walletServiceMock.listWallets).toHaveBeenCalledTimes(1);
    });

    it('should keep the modal open and re-enable the form if saving fails', () => {
        setup();
        walletServiceMock.createWallet.mockReturnValue(throwError(() => new Error('boom')));

        component['openWalletModal'](null);
        fixture.detectChanges();

        getModalStub().saved.emit({
            name: 'Új',
            currencyCode: CurrencyCodesEnum.huf,
            walletType: WalletTypesEnum.default,
        });
        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('app-wallet-modal-component')).toBeTruthy();
        expect(getModalStub().isFormDisabled).toBe(false);
    });

    it('should not delete the wallet when the confirm dialog is declined', () => {
        setup();
        const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);

        component['openWalletModal'](sampleWallet);
        fixture.detectChanges();

        getModalStub().deleted.emit(sampleWallet.id);

        expect(confirmSpy).toHaveBeenCalled();
        expect(walletServiceMock.softDeleteWallet).not.toHaveBeenCalled();

        confirmSpy.mockRestore();
    });

    it('should delete the wallet, reload the list and close the modal when confirmed', () => {
        setup();
        walletServiceMock.listWallets.mockClear();
        const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);

        component['openWalletModal'](sampleWallet);
        fixture.detectChanges();

        getModalStub().deleted.emit(sampleWallet.id);
        TestBed.tick();

        expect(walletServiceMock.softDeleteWallet).toHaveBeenCalledWith(sampleWallet.id);
        expect(walletServiceMock.listWallets).toHaveBeenCalledTimes(1);
        fixture.detectChanges();
        expect(fixture.nativeElement.querySelector('app-wallet-modal-component')).toBeNull();

        confirmSpy.mockRestore();
    });

    it('should close the modal when the modal emits closeModal', () => {
        setup();

        component['openWalletModal'](null);
        fixture.detectChanges();
        expect(fixture.nativeElement.querySelector('app-wallet-modal-component')).toBeTruthy();

        getModalStub().closeModal.emit();
        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('app-wallet-modal-component')).toBeNull();
    });
});
