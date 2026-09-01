import { Component, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { WalletService } from './wallet-service';
import { tap } from 'rxjs/internal/operators/tap';
import { switchMap } from 'rxjs/internal/operators/switchMap';
import { of } from 'rxjs';
import { WalletsListComponent } from './wallets-list-component';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'app-wallets-page-component',
    templateUrl: './wallets-page-component.html',
    styleUrl: './wallets-page-component.scss',
    standalone: true,
    imports: [WalletsListComponent, TranslatePipe],
})
export class WalletsPageComponent {
    private walletService = inject(WalletService);

    private reloadWalletListTrigger = signal(0);
    private selectedWalletId = signal(null);

    protected isWalletListLoading = signal(false);
    protected isWalletDataLoading = signal(false);

    protected walletListData = toSignal(
        toObservable(this.reloadWalletListTrigger).pipe(
            tap(() => this.isWalletListLoading.set(true)),
            switchMap(() =>
                this.walletService
                    .listWallets()
                    .pipe(tap(() => this.isWalletListLoading.set(false))),
            ),
        ),
        { initialValue: [] },
    );

    /**
     * Betölti a kiválasztott wallet adatait
     */
    protected selectedWalletData = toSignal(
        toObservable(this.selectedWalletId).pipe(
            switchMap((walletId) => {
                if (!walletId) {
                    return of(null);
                }
                this.isWalletDataLoading.set(true);
                return this.walletService
                    .getWalletById(walletId)
                    .pipe(tap(() => this.isWalletDataLoading.set(false)));
            }),
        ),
        { initialValue: null },
    );
}
