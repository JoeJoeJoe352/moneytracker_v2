import { Component, inject, signal, WritableSignal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { WalletService } from './wallet-service';
import { tap } from 'rxjs/internal/operators/tap';
import { switchMap } from 'rxjs/internal/operators/switchMap';
import { WalletsListComponent } from './wallets-list-component';
import { _, TranslatePipe, TranslateService } from '@ngx-translate/core';
import { WalletModalComponent } from './wallet-modal-component';
import { WalletCreateRequest, WalletDataInterface, WalletUpdateRequest } from './interfaces';
import { UserDataStore } from '../../shared/services/user-data-store';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatButton } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { WalletFormComponent, WalletFormInputInterface } from './wallet-form-component';

@Component({
    selector: 'app-wallets-page-component',
    templateUrl: './wallets-page-component.html',
    styleUrl: './wallets-page-component.scss',
    standalone: true,
    imports: [
        WalletsListComponent,
        TranslatePipe,
        WalletModalComponent,
        MatProgressSpinner,
        MatCardModule,
        MatButton,
    ],
})
export class WalletsPageComponent {
    private walletService = inject(WalletService);
    private translateService = inject(TranslateService);
    private userData = inject(UserDataStore);
    readonly dialog = inject(MatDialog);

    private reloadWalletListTrigger = signal(0);

    protected isWalletListLoading = signal(false);
    protected isWalletModalOpen = signal(false);
    protected isWalletFormDisabled = signal(false);
    protected selectedWalletData: WritableSignal<WalletDataInterface | null> = signal(null);

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
        this.dialog.open(WalletFormComponent, {
            height: '400px',
            width: '500px',
            data: {
                wallet: walletData,
                isFormDisabled: true,
            } as WalletFormInputInterface,
        });

        //dialogRef.afterClosed().subscribe((result) => {});

        //this.selectedWalletData.set(walletData);
        //this.isWalletModalOpen.set(true);
    }

    /**
     * Bezárja a wallet szerkesztő modalt
     */
    protected closeWalletModal(): void {
        this.isWalletModalOpen.set(false);
        this.selectedWalletData.set(null);
    }

    /**
     * Elmenti a wallet adatait (létrehozás vagy módosítás), majd újratölti a listát
     */
    protected onWalletSaved(payload: WalletCreateRequest | WalletUpdateRequest): void {
        const walletData = this.selectedWalletData();
        const walletId = walletData?.id;
        this.isWalletFormDisabled.set(true);

        const request = walletId
            ? this.walletService.updateWallet(walletId, payload as WalletUpdateRequest)
            : this.walletService.createWallet(payload as WalletCreateRequest);

        request.subscribe({
            next: () => this.afterWalletChange(),
            error: () => this.isWalletFormDisabled.set(false),
        });
    }

    /**
     * Törli a walletet megerősítés után, majd újratölti a listát
     */
    protected onWalletDeleted(walletId: number): void {
        if (!confirm(this.translateService.instant(_('wallet.delete.confirm')))) {
            return;
        }

        this.isWalletFormDisabled.set(true);

        this.walletService.softDeleteWallet(walletId).subscribe({
            next: () => this.afterWalletChange(),
            error: () => this.isWalletFormDisabled.set(false),
        });
    }

    private afterWalletChange(): void {
        this.isWalletFormDisabled.set(false);
        this.reloadWalletListTrigger.update((value) => value + 1);
        this.closeWalletModal();
    }
}
