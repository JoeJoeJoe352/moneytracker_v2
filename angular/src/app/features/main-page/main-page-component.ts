import { Component, inject, resource, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TransactionService } from '../transaction/transaction-service';
import { DecimalPipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { TransactionModalStateService } from '../transaction/transaction-modal-state-service';
import { TransactionsListComponent } from '../transaction/transactions-component';
import { WalletDataUtil } from '@shared/utils/wallet-data-util';
import { StatCardComponent } from './stat-card-component';
import { BalanceCardComponent } from './balance-card-component';
import { TransactionTypeEnum } from '@shared/enums';

@Component({
    selector: 'app-main-page-component',
    templateUrl: './main-page-component.html',
    styleUrl: './main-page-component.scss',
    imports: [TransactionsListComponent, StatCardComponent, BalanceCardComponent],
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
        this.modal.changed
            .pipe(takeUntilDestroyed())
            .subscribe(() => this.reloadAfterTransactionsChanged());
    }

    /**
     * Tranzakció mentése/törlése után a listát és az összesítést is újratöltjük
     */
    private reloadAfterTransactionsChanged(): void {
        this.reloadTransactionListTrigger.update((value) => value + 1);
        this.reloadSummaryList();
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
}
