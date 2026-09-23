import { Component, inject, input, output } from '@angular/core';
import { WalletDataInterface } from './interfaces';
import { WalletDataUtil } from './wallet-data-util';
import { TranslatePipe } from '@ngx-translate/core';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format-pipe';
import { MatRippleModule } from '@angular/material/core';

@Component({
    selector: 'app-wallet-card-component',
    templateUrl: './wallet-card-component.html',
    styleUrl: './wallet-card-component.scss',
    imports: [TranslatePipe, CurrencyFormatPipe, MatRippleModule],
})
export class WalletCardComponent {
    protected readonly walletDataUtil = inject(WalletDataUtil);

    /**
     * Egy wallet adatai
     */
    public walletData = input.required<WalletDataInterface>();

    /**
     * Ha true, a kártya nem kattintható (pl. amíg a lista újratölt)
     */
    public disabled = input(false);

    /**
     * Walletra rákattintott a user
     */
    public cardClicked = output<number>();

    /**
     * Kártyára kattintáskor lefutó műveletek 
     */
    protected onActivate(): void {
        if (this.disabled()) {
            return;
        }
        this.cardClicked.emit(this.walletData().id);
    }
}
