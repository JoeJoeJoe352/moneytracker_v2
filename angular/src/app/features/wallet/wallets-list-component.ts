import { Component, input, output } from '@angular/core';
import { WalletListDataInterface } from './interfaces';
import { WalletCardComponent } from './wallet-card-component';

@Component({
    selector: 'app-wallets-list-component',
    templateUrl: './wallets-list-component.html',
    styleUrl: './wallets-list-component.scss',
    imports: [WalletCardComponent],
})
export class WalletsListComponent {
    /**
     * Wallet lista elemei
     */
    public walletListData = input.required<WalletListDataInterface[]>();

    /**
     * Ha true, a kártyák nem kattinthatók (pl. amíg a lista újratölt)
     */
    public disabled = input(false);

    /**
     * Event, hogy user rákattintott valamelyik elemre
     */
    public walletCardClicked = output<WalletListDataInterface>();

    /**
     * Balance sync event, a modal felnyitására
     */
    public syncRequested = output<WalletListDataInterface>();
}
