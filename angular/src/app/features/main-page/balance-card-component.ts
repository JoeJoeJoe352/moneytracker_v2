import { Component, computed, inject, input, Signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format-pipe';
import { WalletSummaryInterface } from '../transaction/interfaces';
import { TransactionModalStateService } from '../transaction/transaction-modal-state-service';
import { MatCard } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatButton } from '@angular/material/button';

/**
 * Teljes egyenleget és annak e havi változását megjelenítő kártya,
 * tranzakció és átvezetés létrehozása gombokkal
 */
@Component({
    selector: 'app-balance-card-component',
    templateUrl: './balance-card-component.html',
    styleUrls: ['./main-page-component.scss', './balance-card-component.scss'],
    imports: [TranslatePipe, CurrencyFormatPipe, MatCard, MatIcon, MatProgressSpinner, MatButton],
})
export class BalanceCardComponent {
    protected readonly modal = inject(TransactionModalStateService);

    /**
     * Töltődés alatt vannak-e az adatok
     */
    isMoneySumLoading = input.required<boolean>();
    /**
     * Walletek összegei valutánként
     */
    moneySum = input.required<WalletSummaryInterface[]>();
    /**
     * E havi bevételek valutánként
     */
    incomeSumThisMonth = input.required<WalletSummaryInterface[]>();
    /**
     * E havi kiadások valutánként
     */
    expenseSumThisMonth = input.required<WalletSummaryInterface[]>();

    /**
     * Az egyenleg nettó változása ebben a hónapban
     */
    protected balanceChangeThisMonth: Signal<WalletSummaryInterface[]> = computed(() => {
        const income = this.incomeSumThisMonth();
        const expense = this.expenseSumThisMonth();

        const currencyCodes = new Set([
            ...income.map((item) => item.currencyCode),
            ...expense.map((item) => item.currencyCode),
        ]);

        return Array.from(currencyCodes).map((currencyCode) => {
            const incomeTotal =
                income.find((item) => item.currencyCode === currencyCode)?.total ?? 0;
            const expenseTotal =
                expense.find((item) => item.currencyCode === currencyCode)?.total ?? 0;

            return { currencyCode, total: incomeTotal - expenseTotal };
        });
    });
}
