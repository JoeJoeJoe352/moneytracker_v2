import { Component, EventEmitter, Input, Output } from '@angular/core';
import { WalletDataInterface } from './interfaces';
import { WalletCardComponent } from './wallet-card-component';

@Component({
    selector: 'app-wallets-list-component',
    template: `
        <div class="wallet-list">
            @for (wallet of walletListData; track wallet.id) {
                <app-wallet-card-component
                    [walletData]="wallet"
                    (cardClicked)="walletCardClicked.emit(wallet)"
                />
            }
        </div>
    `,
    styleUrl: './wallets-list-component.scss',
    imports: [WalletCardComponent],
    standalone: true,
})
export class WalletsListComponent {
    @Input({ required: true }) walletListData!: WalletDataInterface[];
    @Output() walletCardClicked = new EventEmitter<WalletDataInterface>();
}
