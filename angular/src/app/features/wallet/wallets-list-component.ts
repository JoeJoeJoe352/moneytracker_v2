import { Component, input, output } from '@angular/core';
import { WalletDataInterface } from './interfaces';
import { WalletCardComponent } from './wallet-card-component';

@Component({
    selector: 'app-wallets-list-component',
    template: `
        <div class="wallet-list">
            @for (wallet of walletListData(); track wallet.id) {
                <app-wallet-card-component
                    [walletData]="wallet"
                    [disabled]="disabled()"
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
    /**
     * Wallet lista elemei
     */
    public walletListData = input.required<WalletDataInterface[]>();

    /**
     * Ha true, a kártyák nem kattinthatók (pl. amíg a lista újratölt)
     */
    public disabled = input(false);

    /**
     * Event, hogy user rákattintott valamelyik elemre
     */
    public walletCardClicked = output<WalletDataInterface>();
}
