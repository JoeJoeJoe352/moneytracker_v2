import { Component, computed, Input, WritableSignal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { CurrencyFormatPipe } from '../../shared/pipes/currency-format-pipe';
import { WalletSummaryInterface } from '../transaction/interfaces';
import { TransactionTypeEnum } from '../../shared/enums';

@Component({
    selector: 'app-stat-card-component',
    templateUrl: './stat-card-component.html',
    styleUrls: ['./main-page-component.scss'],
    standalone: true,
    imports: [TranslatePipe, CurrencyFormatPipe],
})
export class StatCardComponent {
    /**
     * Tranzakció típusa
     */
    @Input({ required: true }) transactionType!: TransactionTypeEnum;
    /**
     * Töltődés alatt vannak-e az adatok
     */
    @Input({ required: true }) isMoneySumLoading!: WritableSignal<boolean>;
    /**
     * Kártya adatai
     */
    @Input({ required: true }) walletSummaries!: WalletSummaryInterface[];

    /**
     * Kártya címe, a tranzakció típus függvényében
     */
    protected getCardTitle = computed(() => {
        switch (this.transactionType) {
            case TransactionTypeEnum.INCOME:
                return 'mainpage.total_income';
            case TransactionTypeEnum.OUTCOME:
                return 'mainpage.total_expenses';
        }
    });

    /**
     * Bevétel-e a tranzakció
     */
    protected isIncome = computed(() => {
        return this.transactionType === TransactionTypeEnum.INCOME;
    });
}
