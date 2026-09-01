import { Component, Input } from "@angular/core";
import { WalletDataInterface } from "./interfaces";

@Component({
    selector: 'app-wallets-list-component',
    templateUrl: './wallets-list-component.html',
    styleUrl: './wallets-list-component.scss',
    standalone: true,
})
export class WalletsListComponent {

    @Input({required: true}) walletListData!: WalletDataInterface[];
}