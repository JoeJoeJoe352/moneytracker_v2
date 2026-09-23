import { Component, computed, inject, resource, Signal, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TransactionService } from '../transaction/transaction-service';
import { DecimalPipe } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { TransactionModalStateService } from '../transaction/transaction-modal-state-service';
import { TransactionsListComponent } from '../transaction/transactions-component';
import { WalletDataUtil } from '../wallet/wallet-data-util';
import { WalletSummaryInterface } from '../transaction/interfaces';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format-pipe';
import { StatCardComponent } from './stat-card-component';
import { TransactionTypeEnum } from '@shared/enums';
import { MatCard } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatButton } from '@angular/material/button';

@Component({
    selector: 'app-main-page-component',
    templateUrl: './main-page-component.html',
    styleUrl: './main-page-component.scss',
    imports: [
        TransactionsListComponent,
        TranslatePipe,
        CurrencyFormatPipe,
        StatCardComponent,
        MatCard,
        MatIcon,
        MatProgressSpinner,
        MatButton,
    ],
    providers: [TransactionModalStateService, DecimalPipe],
})
export class MainPageComponent {
    private readonly transactionService = inject(TransactionService);
    protected readonly modal = inject(TransactionModalStateService);
    protected readonly walletUtils = inject(WalletDataUtil);

    protected transactionType = TransactionTypeEnum;
    /**
     * Újra kell-e tölteni az adatokat? Ha ez változik, akkor újra fogja tölteni a listát.
     * Azért számot növelünk és nem boolean értéket, mert ha gyorsan hívódik egymás után,
     * akkor többször true-ra állítódik az érték (nem érkezik false-ra állítani az első lekérés végén) és az nem vált ki új letöltés eventet
     */
    protected reloadTransactionListTrigger = signal(0);

    constructor() {
        this.modal.changed.pipe(takeUntilDestroyed()).subscribe(() => {
            this.reloadTransactionListTrigger.update((value) => value + 1);
            this.reloadSummaryList();
        });
    }

    /**
     * A tranzakciós listában történt változás után újratöltjük az összesítést
     */
    protected reloadSummaryList(): void {
        this.moneySummarizedPerCurrency.reload();
    }

    /**
     * Walletek összegei, valutánként csoportosítva
     */
    protected moneySummarizedPerCurrency = resource({
        loader: async () => {
            const moneySum = await firstValueFrom(this.transactionService.getMoneySum());

            return {
                moneySum: this.walletUtils.summarizeSumPerCurrency(moneySum.moneySum),
                expenseSumThisMonth: this.walletUtils.summarizeSumPerCurrency(
                    moneySum.expenseSumThisMonth,
                ),
                incomeSumThisMonth: this.walletUtils.summarizeSumPerCurrency(
                    moneySum.incomeSumThisMonth,
                ),
            };
        },
    });

    /**
     * Az egyenleg nettó változása ebben a hónapban
     */
    protected balanceChangeThisMonth: Signal<WalletSummaryInterface[]> = computed(() => {
        const income = this.moneySummarizedPerCurrency.value()?.incomeSumThisMonth;
        const expense = this.moneySummarizedPerCurrency.value()?.expenseSumThisMonth;
        if (!income || !expense) {
            return [];
        }

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
