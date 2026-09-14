import { Component, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { WalletService } from './wallet-service';
import { tap } from 'rxjs/internal/operators/tap';
import { switchMap } from 'rxjs/internal/operators/switchMap';
import { WalletsListComponent } from './wallets-list-component';
import { _, TranslatePipe, TranslateService } from '@ngx-translate/core';
import { WalletCreateRequest, WalletDataInterface, WalletUpdateRequest } from './interfaces';
import { UserDataStore } from '../../shared/services/user-data-store';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatButton } from '@angular/material/button';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { WalletFormComponent, WalletFormInputInterface } from './wallet-form-component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog-component';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
    selector: 'app-wallets-page-component',
    templateUrl: './wallets-page-component.html',
    styleUrl: './wallets-page-component.scss',
    standalone: true,
    imports: [WalletsListComponent, TranslatePipe, MatProgressSpinner, MatCardModule, MatButton],
})
export class WalletsPageComponent {
    private readonly walletService = inject(WalletService);
    private readonly translateService = inject(TranslateService);
    private readonly userData = inject(UserDataStore);
    private readonly dialog = inject(MatDialog);
    private readonly snackBar = inject(MatSnackBar);

    /**
     * Wallet lista újratöltéséhez való signal (növelni az értékét és újratöltődik)
     */
    private reloadWalletListTrigger = signal(0);
    /**
     * Wallet lista töltődik-e jelenleg
     */
    protected isWalletListLoading = signal(false);
    /**
     * Wallet form írásvédett-e (Pl.: mentéskor)
     */
    protected isWalletFormDisabled = signal(false);
    /**
     * Wallet adatokat tároló változó
     */
    protected walletListData = toSignal(
        toObservable(this.reloadWalletListTrigger).pipe(
            tap(() => this.isWalletListLoading.set(true)),
            switchMap(() =>
                this.walletService.listWallets().pipe(
                    tap((wallets) => {
                        this.isWalletListLoading.set(false);
                        // store-t is befrissítjük mellékhatásként, a sum nélkül
                        this.userData.setWallets(
                            wallets.map((wallet) => ({
                                id: wallet.id,
                                name: wallet.name,
                                currencyCode: wallet.currencyCode,
                                type: wallet.type,
                            })),
                        );
                    }),
                ),
            ),
        ),
        { initialValue: [] },
    );

    /**
     * Megnyitja a wallet létrehozó/szerkesztő modalt. Ha van id, akkor szerkesztés, egyébként létrehozás
     */
    protected openWalletModal(walletData: WalletDataInterface | null): void {
        const dialogRef = this.dialog.open(WalletFormComponent, {
            width: '500px',
            data: {
                wallet: walletData,
                isFormDisabled: this.isWalletFormDisabled,
            } as WalletFormInputInterface,
        });

        dialogRef.componentInstance.saved.subscribe((payload) =>
            this.onWalletSaved(walletData, payload, dialogRef),
        );
        dialogRef.componentInstance.deleted.subscribe((walletId) =>
            this.onWalletDeleted(walletId, dialogRef),
        );
    }

    /**
     * Elmenti a wallet adatait (létrehozás vagy módosítás), majd újratölti a listát
     */
    private onWalletSaved(
        existingWalletData: WalletDataInterface | null,
        walletDataToSave: WalletCreateRequest | WalletUpdateRequest,
        dialogRef: MatDialogRef<WalletFormComponent>,
    ): void {
        const isEditMode = existingWalletData !== null;
        this.isWalletFormDisabled.set(true);

        const request = isEditMode
            ? this.walletService.updateWallet(
                  existingWalletData.id,
                  walletDataToSave as WalletUpdateRequest,
              )
            : this.walletService.createWallet(walletDataToSave as WalletCreateRequest);

        this.handleWalletRequest(request, _('etc.save-success'), dialogRef);
    }

    /**
     * Törli a walletet megerősítés után, majd újratölti a listát
     */
    private onWalletDeleted(walletId: number, dialogRef: MatDialogRef<WalletFormComponent>): void {
        this.dialog
            .open(ConfirmDialogComponent, {
                data: {
                    message: this.translateService.instant(_('wallet.delete.confirm')),
                    danger: true,
                },
            })
            .afterClosed()
            .subscribe((confirmed) => {
                if (!confirmed) {
                    return;
                }

                this.isWalletFormDisabled.set(true);

                this.handleWalletRequest(
                    this.walletService.softDeleteWallet(walletId),
                    _('etc.delete-success'),
                    dialogRef,
                );
            });
    }

    /**
     * Egy wallet létrehozó/módosító/törlő kérés lefutása utáni műveleteket végzi el
     * (user tájékoztatás, hiba logolás, stb...)
     */
    private handleWalletRequest(
        request: Observable<unknown>,
        successMessageKey: string,
        dialogRef: MatDialogRef<WalletFormComponent>,
    ): void {
        request.subscribe({
            next: () => {
                this.snackBar.open(
                    this.translateService.instant(successMessageKey),
                    this.translateService.instant(_('etc.close')),
                );
                this.afterWalletChange(dialogRef);
            },
            error: (error) => {
                console.error(error);
                this.snackBar.open(
                    this.translateService.instant(_('etc.general-error')),
                    this.translateService.instant(_('etc.close')),
                );
                this.isWalletFormDisabled.set(false);
            },
        });
    }

    /**
     * Műveletek, ami wallet mentés után kell végrehajtani
     */
    private afterWalletChange(dialogRef: MatDialogRef<WalletFormComponent>): void {
        this.isWalletFormDisabled.set(false);
        this.reloadWalletListTrigger.update((value) => value + 1);
        dialogRef.close();
    }
}
