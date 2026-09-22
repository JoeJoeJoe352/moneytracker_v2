import { Component, inject, resource } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import { WalletService } from './wallet-service';
import { WalletsListComponent } from './wallets-list-component';
import { _, TranslatePipe, TranslateService } from '@ngx-translate/core';
import { WalletCreateRequest, WalletDataInterface, WalletUpdateRequest } from './interfaces';
import { UserDataStore } from '@shared/services/user-data-store';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatButton } from '@angular/material/button';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { WalletFormComponent, WalletFormInputInterface } from './wallet-form-component';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog-component';
import { NotificationService } from '@shared/services/notification-service';
import { RESOURCE_STATUS_LOADING } from '@shared/constants';

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
    private readonly notification = inject(NotificationService);

    protected readonly RESOURCE_STATUS_LOADING = RESOURCE_STATUS_LOADING;

    /**
     * Wallet adatok
     */
    protected walletListResource = resource({
        defaultValue: [],
        loader: async () => {
            const wallets = await firstValueFrom(this.walletService.listWallets());

            // store frissítés mellékhatásként
            this.userData.setWallets(
                wallets.map((wallet) => ({
                    id: wallet.id,
                    name: wallet.name,
                    currencyCode: wallet.currencyCode,
                    type: wallet.type,
                })),
            );
            return wallets;
        },
    });

    /**
     * Megnyitja a wallet létrehozó/szerkesztő modalt. Ha van id, akkor szerkesztés, egyébként létrehozás
     */
    protected openWalletModal(walletData: WalletDataInterface | null): void {
        const dialogRef = this.dialog.open(WalletFormComponent, {
            restoreFocus: true,
            width: '500px',
            data: {
                wallet: walletData,
                isFormDisabled: this.walletListResource.isLoading,
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
        ConfirmDialogComponent.open(this.dialog, {
            message: this.translateService.instant(_('wallet.delete.confirm')),
            danger: true,
        })
            .afterClosed()
            .subscribe((confirmed) => {
                if (!confirmed) {
                    return;
                }

                this.handleWalletRequest(
                    this.walletService.softDeleteWallet(walletId),
                    _('etc.delete-success'),
                    dialogRef,
                );
            });
    }

    /**
     * Egy wallet létrehozó/módosító/törlő kérés lefutása utáni műveleteket végzi el
     * (user tájékoztatás, hiba logolás, modal bezárás, lista frissítés, stb...)
     */
    private handleWalletRequest(
        request: Observable<unknown>,
        successMessageKey: string,
        dialogRef: MatDialogRef<WalletFormComponent>,
    ): void {
        request.subscribe({
            next: () => {
                this.notification.show(successMessageKey);
                this.walletListResource.reload();
                dialogRef.close();
            },
            error: (error) => {
                console.error(error);
                this.notification.showGeneralError();
            },
        });
    }
}
