import { Component, EventEmitter, Input, Output } from '@angular/core';
import { WalletDataInterface } from './interfaces';
import { WalletCardComponent } from './wallet-card-component';

@Component({
    selector: 'app-wallets-list-component',
    template: `
        <div class="d-flex card-list flex-wrap column-gap-3 row-gap-3 justify-content-center">
            @for (wallet of walletListData; track wallet.id) {
                <app-wallet-card-component
                    [walletData]="wallet"
                    (cardClicked)="walletCardClicked.emit(wallet)"
                />
            }
        </div>
    `,
    imports: [WalletCardComponent],
    standalone: true,
})
export class WalletsListComponent {
    @Input({ required: true }) walletListData!: WalletDataInterface[];
    @Output() walletCardClicked = new EventEmitter<WalletDataInterface>();
}
