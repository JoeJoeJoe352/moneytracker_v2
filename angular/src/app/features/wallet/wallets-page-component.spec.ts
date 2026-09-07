import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Observable, of, Subject, throwError } from 'rxjs';
import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatButton } from '@angular/material/button';
import { WalletsPageComponent } from './wallets-page-component';
import { WalletFormComponent } from './wallet-form-component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog-component';
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

/**
 * A MatDialog.open() valós helyett használt, kézzel vezérelhető dialogRef, ami lehetővé teszi a
 * dialog "componentInstance"-ének (kimenő eseményeinek) és afterClosed()-jének szimulálását,
 * anélkül hogy a valós MatDialog/CDK overlay-t kellene betöltenünk a tesztekhez.
 */
class FakeDialogRef<T> {
    close = vi.fn((result?: unknown) => this.closedSubject.next(result));
    private closedSubject = new Subject<unknown>();

    constructor(public componentInstance: T) {}

    afterClosed(): Observable<unknown> {
        return this.closedSubject.asObservable();
    }
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
    let dialogOpenSpy: ReturnType<typeof vi.fn>;
    let walletFormDialogRefs: FakeDialogRef<{
        saved: EventEmitter<WalletCreateRequest | WalletUpdateRequest>;
        deleted: EventEmitter<number>;
    }>[];
    let confirmDialogRefs: FakeDialogRef<unknown>[];

    function setup(listWalletsResult: Observable<WalletDataInterface[]> = of([sampleWallet])) {
        walletServiceMock = {
            listWallets: vi.fn(() => listWalletsResult),
            createWallet: vi.fn(() => of(undefined)),
            updateWallet: vi.fn(() => of(undefined)),
            softDeleteWallet: vi.fn(() => of(undefined)),
        };
        walletFormDialogRefs = [];
        confirmDialogRefs = [];

        dialogOpenSpy = vi.fn((componentType: unknown) => {
            if (componentType === WalletFormComponent) {
                const dialogRef = new FakeDialogRef({
                    saved: new EventEmitter<WalletCreateRequest | WalletUpdateRequest>(),
                    deleted: new EventEmitter<number>(),
                });
                walletFormDialogRefs.push(dialogRef);
                return dialogRef;
            }
            if (componentType === ConfirmDialogComponent) {
                const dialogRef = new FakeDialogRef(undefined);
                confirmDialogRefs.push(dialogRef);
                return dialogRef;
            }
            throw new Error('Unexpected dialog component opened: ' + String(componentType));
        });

        TestBed.configureTestingModule({
            imports: [WalletsPageComponent],
            providers: [
                provideTranslateService(),
                { provide: WalletService, useValue: walletServiceMock },
                { provide: MatDialog, useValue: { open: dialogOpenSpy } },
            ],
        });
        TestBed.overrideComponent(WalletsPageComponent, {
            set: {
                imports: [
                    StubWalletsListComponent,
                    TranslatePipe,
                    MatCardModule,
                    MatProgressSpinner,
                    MatButton,
                ],
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

    function lastWalletFormDialogRef() {
        return walletFormDialogRefs[walletFormDialogRefs.length - 1];
    }

    function lastConfirmDialogRef() {
        return confirmDialogRefs[confirmDialogRefs.length - 1];
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
        expect(fixture.nativeElement.querySelector('mat-spinner')).toBeTruthy();
        expect(fixture.nativeElement.querySelector('app-wallets-list-component')).toBeNull();
    });

    it('should not open any dialog until the create button is clicked', () => {
        setup();

        expect(dialogOpenSpy).not.toHaveBeenCalled();
    });

    it('should open the wallet form dialog in create mode (no wallet) when the create button is clicked', () => {
        setup();

        const createButton = fixture.nativeElement.querySelector('button');
        createButton.click();

        expect(dialogOpenSpy).toHaveBeenCalledWith(
            WalletFormComponent,
            expect.objectContaining({ data: expect.objectContaining({ wallet: null }) }),
        );
    });

    it('should open the wallet form dialog in edit mode with the clicked wallet when a card is clicked', () => {
        setup();

        getListStub().walletCardClicked.emit(sampleWallet);

        expect(dialogOpenSpy).toHaveBeenCalledWith(
            WalletFormComponent,
            expect.objectContaining({ data: expect.objectContaining({ wallet: sampleWallet }) }),
        );
    });

    it('should create a new wallet, reload the list and close the dialog', () => {
        setup();
        walletServiceMock.listWallets.mockClear();

        component['openWalletModal'](null);

        const payload: WalletCreateRequest = {
            name: 'Új tárca',
            currencyCode: CurrencyCodesEnum.huf,
            walletType: WalletTypesEnum.default,
        };
        lastWalletFormDialogRef().componentInstance.saved.emit(payload);
        TestBed.tick();

        expect(walletServiceMock.createWallet).toHaveBeenCalledWith(payload);
        expect(walletServiceMock.updateWallet).not.toHaveBeenCalled();
        expect(walletServiceMock.listWallets).toHaveBeenCalledTimes(1);
        expect(lastWalletFormDialogRef().close).toHaveBeenCalled();
    });

    it('should update the selected wallet and reload the list', () => {
        setup();
        walletServiceMock.listWallets.mockClear();

        component['openWalletModal'](sampleWallet);

        const payload: WalletUpdateRequest = {
            name: 'Módosított',
            walletType: WalletTypesEnum.savings,
        };
        lastWalletFormDialogRef().componentInstance.saved.emit(payload);
        TestBed.tick();

        expect(walletServiceMock.updateWallet).toHaveBeenCalledWith(sampleWallet.id, payload);
        expect(walletServiceMock.createWallet).not.toHaveBeenCalled();
        expect(walletServiceMock.listWallets).toHaveBeenCalledTimes(1);
    });

    it('should keep the dialog open and re-enable the form if saving fails', () => {
        setup();
        walletServiceMock.createWallet.mockReturnValue(throwError(() => new Error('boom')));

        component['openWalletModal'](null);
        lastWalletFormDialogRef().componentInstance.saved.emit({
            name: 'Új',
            currencyCode: CurrencyCodesEnum.huf,
            walletType: WalletTypesEnum.default,
        });

        expect(lastWalletFormDialogRef().close).not.toHaveBeenCalled();
        expect(component['isWalletFormDisabled']()).toBe(false);
    });

    it('should not delete the wallet when the confirm dialog is declined', () => {
        setup();

        component['openWalletModal'](sampleWallet);
        lastWalletFormDialogRef().componentInstance.deleted.emit(sampleWallet.id);

        expect(dialogOpenSpy).toHaveBeenCalledWith(ConfirmDialogComponent, expect.anything());
        lastConfirmDialogRef().close(false);

        expect(walletServiceMock.softDeleteWallet).not.toHaveBeenCalled();
    });

    it('should delete the wallet, reload the list and close the dialog when confirmed', () => {
        setup();
        walletServiceMock.listWallets.mockClear();

        component['openWalletModal'](sampleWallet);
        const formDialogRef = lastWalletFormDialogRef();
        formDialogRef.componentInstance.deleted.emit(sampleWallet.id);
        lastConfirmDialogRef().close(true);
        TestBed.tick();

        expect(walletServiceMock.softDeleteWallet).toHaveBeenCalledWith(sampleWallet.id);
        expect(walletServiceMock.listWallets).toHaveBeenCalledTimes(1);
        expect(formDialogRef.close).toHaveBeenCalled();
    });
});
