import { Component, EventEmitter, inject, Input, Output } from '@angular/core';
import { WalletDataInterface } from './interfaces';
import { WalletDataUtil } from './wallet-data-util';
import { TranslatePipe } from '@ngx-translate/core';
import { CurrencyFormatPipe } from '../../shared/pipes/currency-format-pipe';
import { MatRippleModule } from '@angular/material/core';

@Component({
    selector: 'app-wallet-card-component',
    templateUrl: './wallet-card-component.html',
    styleUrl: './wallet-card-component.scss',
    standalone: true,
    imports: [TranslatePipe, CurrencyFormatPipe, MatRippleModule],
})
export class WalletCardComponent {
    protected readonly walletDataUtil = inject(WalletDataUtil);

    @Input({ required: true }) walletData!: WalletDataInterface;
    @Output() cardClicked = new EventEmitter<number>();
}
