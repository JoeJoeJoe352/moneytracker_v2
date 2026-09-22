import { Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format-pipe';
import { WalletSummaryInterface } from '../transaction/interfaces';
import { TransactionTypeEnum } from '@shared/enums';
import { MatCard } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

@Component({
    selector: 'app-stat-card-component',
    templateUrl: './stat-card-component.html',
    styleUrls: ['./main-page-component.scss'],
    standalone: true,
    imports: [TranslatePipe, CurrencyFormatPipe, MatCard, MatIcon, MatProgressSpinner],
})
export class StatCardComponent {
    /**
     * Tranzakció típusa
     */
    transactionType = input.required<TransactionTypeEnum>()
    /**
     * Töltődés alatt vannak-e az adatok
    */
    isMoneySumLoading = input.required<boolean>()
    /**
     * Kártya adatai
    */
    walletSummaries = input.required<WalletSummaryInterface[]>()

    /**
     * Kártya címe, a tranzakció típus függvényében
     */
    protected getCardTitle = computed(() => {
        switch (this.transactionType()) {
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
        return this.transactionType() === TransactionTypeEnum.INCOME;
    });
}
