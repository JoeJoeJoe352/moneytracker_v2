import { Component, computed, inject, input, output } from '@angular/core';
import { WalletDataInterface } from './interfaces';
import { WalletDataUtil } from './wallet-data-util';
import { TranslatePipe } from '@ngx-translate/core';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format-pipe';
import { MatRippleModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

const MS_PER_DAY = 24 * 60 * 60 * 1000;

/**
 * Ennyi nap után számít elavultnak az utolsó szinkronizáció
 */
const SYNC_OUTDATED_AFTER_DAYS = 30;

@Component({
    selector: 'app-wallet-card-component',
    templateUrl: './wallet-card-component.html',
    styleUrl: './wallet-card-component.scss',
    imports: [MatButtonModule, TranslatePipe, CurrencyFormatPipe, MatRippleModule, MatIconModule],
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
    public cardClicked = output<void>();
    /**
     * Szinkronizációs gombra kattintott-e a user
     */
    public syncRequested = output<void>();

    /**
     * Kártyára kattintáskor lefutó műveletek
     */
    protected onActivate(): void {
        if (this.disabled()) {
            return;
        }
        this.cardClicked.emit();
    }

    /**
     * 30 napnál régebben volt az utolsó szinkron?
     */
    protected isSyncOutdated = computed(() => {
        // a "T00:00:00" miatt helyi idő szerinti éjfélként értelmezi; a puszta "YYYY-MM-DD"-t
        // UTC-ként olvasná, ami negatív időzónában az előző napra csúszna
        const lastSyncDate = new Date(`${this.walletData().lastSyncDate}T00:00:00`);
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        // kerekítés: óraátállításkor egy nap 23 vagy 25 óra
        const daysSinceLastSync = Math.round(
            (today.getTime() - lastSyncDate.getTime()) / MS_PER_DAY,
        );
        return daysSinceLastSync > SYNC_OUTDATED_AFTER_DAYS;
    });
}
