import { Component, inject, Input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { WalletDataInterface } from './interfaces';
import { WalletDataUtil } from './wallet-data-util';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'app-wallet-card-component',
    templateUrl: './wallet-card-component.html',
    styleUrl: './wallet-card-component.scss',
    standalone: true,
    imports: [DecimalPipe, TranslatePipe],
})
export class WalletCardComponent {
    protected readonly walletDataUtil = inject(WalletDataUtil);

    @Input({ required: true }) walletData!: WalletDataInterface;

}
