import { describe, it, expect, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, EventEmitter, input, output } from '@angular/core';
import { Observable, of, Subject, throwError } from 'rxjs';
import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatButton } from '@angular/material/button';
import { WalletsPageComponent } from './wallets-page-component';
import { WalletFormComponent } from './wallet-form-component';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog-component';
import { WalletService } from './wallet-service';
import { WalletSyncFormComponent } from './wallet-sync-form-component';
import {
    WalletDataInterface,
    WalletCreateRequest,
    WalletSyncData,
    WalletSyncFormInputInterface,
    WalletUpdateRequest,
} from './interfaces';
import { CurrencyCodesEnum, WalletTypesEnum } from '@shared/enums';

@Component({
    selector: 'app-wallets-list-component',
    template: '',
})
class StubWalletsListComponent {
    walletListData = input<WalletDataInterface[]>([]);

    disabled = input(false);

    walletCardClicked = output<WalletDataInterface>();

    syncRequested = output<WalletDataInterface>();
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
    lastSyncDate: '2026-09-15',
};

describe('WalletsPageComponent (Vitest)', () => {
    let fixture: ComponentFixture<WalletsPageComponent>;
    let component: WalletsPageComponent;
    let walletServiceMock: {
        listWallets: ReturnType<typeof vi.fn>;
        createWallet: ReturnType<typeof vi.fn>;
        updateWallet: ReturnType<typeof vi.fn>;
        softDeleteWallet: ReturnType<typeof vi.fn>;
        syncWallet: ReturnType<typeof vi.fn>;
    };
    let dialogOpenSpy: ReturnType<typeof vi.fn>;
    let walletFormDialogRefs: FakeDialogRef<{
        saved: EventEmitter<WalletCreateRequest | WalletUpdateRequest>;
        deleted: EventEmitter<number>;
    }>[];
    let confirmDialogRefs: FakeDialogRef<unknown>[];
    let syncDialogRefs: FakeDialogRef<{ syncFormSended: EventEmitter<WalletSyncData> }>[];
    let syncDialogData: WalletSyncFormInputInterface | undefined;

    async function setup(listWalletsResult: Observable<WalletDataInterface[]> = of([sampleWallet])) {
        walletServiceMock = {
            listWallets: vi.fn(() => listWalletsResult),
            createWallet: vi.fn(() => of(undefined)),
            updateWallet: vi.fn(() => of(undefined)),
            softDeleteWallet: vi.fn(() => of(undefined)),
            syncWallet: vi.fn(() => of(undefined)),
        };
        walletFormDialogRefs = [];
        confirmDialogRefs = [];
        syncDialogRefs = [];
        syncDialogData = undefined;

        dialogOpenSpy = vi.fn((componentType: unknown, config?: { data?: unknown }) => {
            if (componentType === WalletSyncFormComponent) {
                const dialogRef = new FakeDialogRef({
                    syncFormSended: new EventEmitter<WalletSyncData>(),
                });
                syncDialogRefs.push(dialogRef);
                syncDialogData = config?.data as WalletSyncFormInputInterface;
                return dialogRef;
            }
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
        await fixture.whenStable();
    }

    function getListStub(): StubWalletsListComponent {
        return fixture.debugElement.query(
            (de) => de.componentInstance instanceof StubWalletsListComponent,
        ).componentInstance as StubWalletsListComponent;
    }

    function lastWalletFormDialogRef() {
        const dialogRef = walletFormDialogRefs.at(-1);
        if (!dialogRef) {
            throw new Error('No wallet form dialog was opened');
        }
        return dialogRef;
    }

    function lastSyncDialogRef() {
        const dialogRef = syncDialogRefs.at(-1);
        if (!dialogRef) {
            throw new Error('No sync dialog was opened');
        }
        return dialogRef;
    }

    function lastConfirmDialogRef() {
        const dialogRef = confirmDialogRefs.at(-1);
        if (!dialogRef) {
            throw new Error('No confirm dialog was opened');
        }
        return dialogRef;
    }

    it('should load the wallet list on init', async () => {
        await setup();

        expect(walletServiceMock.listWallets).toHaveBeenCalledTimes(1);
        expect(getListStub().walletListData()).toEqual([sampleWallet]);
    });

    // A resource() maga kezeli a hibaállapotot: sikertelen betöltés után isLoading() false lesz
    // (a spinner eltűnik), a lista pedig üresen jelenik meg ahelyett, hogy a hibás resource.value()
    // hívás elszállna.
    it('should stop loading and render an empty list if loading the wallets fails', async () => {
        await setup(throwError(() => new Error('boom')));

        expect(component['walletListResource'].isLoading()).toBe(false);
        expect(fixture.nativeElement.querySelector('mat-spinner')).toBeNull();
        expect(getListStub().walletListData()).toEqual([]);
    });

    // Reload alatt a lista a DOM-ban marad (így a dialog restoreFocus-a működik), de le van tiltva
    it('should keep the list rendered but disabled while the wallets are reloading', async () => {
        await setup();
        const stub = getListStub();
        expect(stub.disabled()).toBe(false);

        walletServiceMock.listWallets.mockReturnValue(new Subject<WalletDataInterface[]>());
        component['walletListResource'].reload();
        fixture.detectChanges();

        expect(getListStub()).toBe(stub);
        expect(stub.disabled()).toBe(true);
        expect(fixture.nativeElement.querySelector('mat-spinner')).toBeNull();
    });

    it('should disable the create button while the wallets are loading', async () => {
        await setup();
        const createButton = fixture.nativeElement.querySelector('button');
        expect(createButton.disabled).toBe(false);

        walletServiceMock.listWallets.mockReturnValue(new Subject<WalletDataInterface[]>());
        component['walletListResource'].reload();
        fixture.detectChanges();

        expect(createButton.disabled).toBe(true);
    });

    it('should not open any dialog until the create button is clicked', async () => {
        await setup();

        expect(dialogOpenSpy).not.toHaveBeenCalled();
    });

    it('should open the wallet form dialog in create mode (no wallet) when the create button is clicked', async () => {
        await setup();

        const createButton = fixture.nativeElement.querySelector('button');
        createButton.click();

        expect(dialogOpenSpy).toHaveBeenCalledWith(
            WalletFormComponent,
            expect.objectContaining({ data: expect.objectContaining({ wallet: null }) }),
        );
    });

    it('should open the wallet form dialog in edit mode with the clicked wallet when a card is clicked', async () => {
        await setup();

        getListStub().walletCardClicked.emit(sampleWallet);

        expect(dialogOpenSpy).toHaveBeenCalledWith(
            WalletFormComponent,
            expect.objectContaining({ data: expect.objectContaining({ wallet: sampleWallet }) }),
        );
    });

    it('should create a new wallet, reload the list and close the dialog', async () => {
        await setup();
        walletServiceMock.listWallets.mockClear();

        component['openWalletModal'](null);

        const payload: WalletCreateRequest = {
            name: 'Új tárca',
            currencyCode: CurrencyCodesEnum.huf,
            walletType: WalletTypesEnum.default,
        };
        lastWalletFormDialogRef().componentInstance.saved.emit(payload);
        await fixture.whenStable();

        expect(walletServiceMock.createWallet).toHaveBeenCalledWith(payload);
        expect(walletServiceMock.updateWallet).not.toHaveBeenCalled();
        expect(walletServiceMock.listWallets).toHaveBeenCalledTimes(1);
        expect(lastWalletFormDialogRef().close).toHaveBeenCalled();
    });

    it('should update the selected wallet and reload the list', async () => {
        await setup();
        walletServiceMock.listWallets.mockClear();

        component['openWalletModal'](sampleWallet);

        const payload: WalletUpdateRequest = {
            name: 'Módosított',
            walletType: WalletTypesEnum.savings,
        };
        lastWalletFormDialogRef().componentInstance.saved.emit(payload);
        await fixture.whenStable();

        expect(walletServiceMock.updateWallet).toHaveBeenCalledWith(sampleWallet.id, payload);
        expect(walletServiceMock.createWallet).not.toHaveBeenCalled();
        expect(walletServiceMock.listWallets).toHaveBeenCalledTimes(1);
    });

    it('should keep the dialog open and re-enable the form if saving fails', async () => {
        await setup();
        walletServiceMock.createWallet.mockReturnValue(throwError(() => new Error('boom')));

        component['openWalletModal'](null);
        lastWalletFormDialogRef().componentInstance.saved.emit({
            name: 'Új',
            currencyCode: CurrencyCodesEnum.huf,
            walletType: WalletTypesEnum.default,
        });

        expect(lastWalletFormDialogRef().close).not.toHaveBeenCalled();
        expect(component['walletListResource'].isLoading()).toBe(false);
    });

    it('should not delete the wallet when the confirm dialog is declined', async () => {
        await setup();

        component['openWalletModal'](sampleWallet);
        lastWalletFormDialogRef().componentInstance.deleted.emit(sampleWallet.id);

        expect(dialogOpenSpy).toHaveBeenCalledWith(ConfirmDialogComponent, expect.anything());
        lastConfirmDialogRef().close(false);

        expect(walletServiceMock.softDeleteWallet).not.toHaveBeenCalled();
    });

    it('should delete the wallet, reload the list and close the dialog when confirmed', async () => {
        await setup();
        walletServiceMock.listWallets.mockClear();

        component['openWalletModal'](sampleWallet);
        const formDialogRef = lastWalletFormDialogRef();
        formDialogRef.componentInstance.deleted.emit(sampleWallet.id);
        lastConfirmDialogRef().close(true);
        await fixture.whenStable();

        expect(walletServiceMock.softDeleteWallet).toHaveBeenCalledWith(sampleWallet.id);
        expect(walletServiceMock.listWallets).toHaveBeenCalledTimes(1);
        expect(formDialogRef.close).toHaveBeenCalled();
    });

    it('should open the sync dialog with the wallet when a sync is requested from the list', async () => {
        await setup();

        getListStub().syncRequested.emit(sampleWallet);

        expect(dialogOpenSpy).toHaveBeenCalledWith(
            WalletSyncFormComponent,
            expect.objectContaining({ data: expect.objectContaining({ wallet: sampleWallet }) }),
        );
    });

    it('should sync the wallet, reload the list and close the sync dialog', async () => {
        await setup();
        walletServiceMock.listWallets.mockClear();

        component['openSyncModal'](sampleWallet);
        const payload: WalletSyncData = { walletId: sampleWallet.id, currentBalance: 1500 };
        lastSyncDialogRef().componentInstance.syncFormSended.emit(payload);
        await fixture.whenStable();

        expect(walletServiceMock.syncWallet).toHaveBeenCalledWith(payload);
        expect(walletServiceMock.listWallets).toHaveBeenCalledTimes(1);
        expect(lastSyncDialogRef().close).toHaveBeenCalled();
    });

    it('should keep the sync dialog open and re-enable the form if syncing fails', async () => {
        await setup();
        walletServiceMock.syncWallet.mockReturnValue(throwError(() => new Error('boom')));

        component['openSyncModal'](sampleWallet);
        lastSyncDialogRef().componentInstance.syncFormSended.emit({
            walletId: sampleWallet.id,
            currentBalance: 1500,
        });

        expect(lastSyncDialogRef().close).not.toHaveBeenCalled();
        expect(syncDialogData?.isLoading()).toBe(false);
    });

    // amíg a kérés fut, a form le van tiltva, és az újabb submit nem indít még egy szinkront
    it('should disable the sync form and ignore repeated submits while the sync request is pending', async () => {
        await setup();
        const pendingRequest = new Subject<void>();
        walletServiceMock.syncWallet.mockReturnValue(pendingRequest);

        component['openSyncModal'](sampleWallet);
        const payload: WalletSyncData = { walletId: sampleWallet.id, currentBalance: 1500 };
        lastSyncDialogRef().componentInstance.syncFormSended.emit(payload);
        lastSyncDialogRef().componentInstance.syncFormSended.emit(payload);

        expect(walletServiceMock.syncWallet).toHaveBeenCalledTimes(1);
        expect(syncDialogData?.isLoading()).toBe(true);

        pendingRequest.error(new Error('boom'));

        expect(syncDialogData?.isLoading()).toBe(false);
    });
});
