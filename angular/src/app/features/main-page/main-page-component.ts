import { Component, computed, inject, Signal, signal, WritableSignal } from '@angular/core';
import { TransactionService } from '../transaction/transaction-service';
import { DecimalPipe } from '@angular/common';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { switchMap, tap } from 'rxjs';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { TransactionModalStateService } from '../transaction/transaction-modal-state-service';
import { TransactionsListComponent } from '../transaction/transactions-component';
import { TransactionModalComponent } from '../transaction/transaction-modal';
import { WalletDataUtil } from '../wallet/wallet-data-util';
import { MoneySumInterface, WalletSummaryInterface } from '../transaction/interfaces';
import { CurrencySymbolPipe } from '../../shared/pipes/currency-symbol-pipe';

@Component({
    selector: 'app-main-page-component',
    templateUrl: './main-page-component.html',
    styleUrl: './main-page-component.scss',
    standalone: true,
    imports: [
        TransactionsListComponent,
        DecimalPipe,
        TranslatePipe,
        TransactionModalComponent,
        CurrencySymbolPipe,
    ],
    providers: [TransactionModalStateService, DecimalPipe],
})
export class MainPage {
    private transactionService = inject(TransactionService);
    private translateService = inject(TranslateService);
    private decimalPipe = inject(DecimalPipe);
    protected modal = inject(TransactionModalStateService);
    protected walletUtils = inject(WalletDataUtil);

    /**
     * Újra kell-e tölteni az adatokat? Ha ez változik, akkor újra fogja tölteni a listát.
     * Azért számot növelünk és nem boolean értéket, mert ha gyorsan hívódik egymás után,
     * akkor többször true-ra állítódik az érték és az nem vált ki új letöltés eventet
     */
    protected reloadTransactionListTrigger = signal(0);

    /**
     * Ha ez az érték változik, újratöltjük az összesítést. Külön jelzőérték a listától,
     * mert az összesítést a lista saját maga is módosíthatja (pl. szerkesztés/törlés a listában),
     * nem csak az itt lévő "+ tranzakció létrehozása" gomb
     */
    private reloadMoneySumTrigger = signal(0);

    /**
     * Összes pénz töltődik-e
     */
    protected isMoneySumLoading = signal(true);

    /**
     * Az összesített pénz, valutánként
     */
    protected moneySumSummarizedPerCurrency: WritableSignal<MoneySumInterface | null> =
        signal(null);

    constructor() {
        // Mentés/törlés után újratöltjük a listát és az összesítést
        this.modal.changed.subscribe(() => {
            this.reloadTransactionListTrigger.update((value) => value + 1);
            this.reloadMoneySumTrigger.update((value) => value + 1);
        });
    }

    /**
     * A tranzakciós listában történt változás után újratöltjük az összesítést
     */
    protected onListTransactionsChanged(): void {
        this.reloadMoneySumTrigger.update((value) => value + 1);
    }

    /**
     * Felhasználó összes pénze.
     */
    protected moneySum = toSignal(
        toObservable(this.reloadMoneySumTrigger).pipe(
            tap(() => this.isMoneySumLoading.set(true)),
            switchMap(() =>
                this.transactionService.getMoneySum().pipe(
                    tap((walletData) => {
                        this.isMoneySumLoading.set(false);
                        this.moneySumSummarizedPerCurrency.set({
                            moneySum: this.walletUtils.summarizeSumPerCurrency(walletData.moneySum),
                            expenseSumThisMonth: this.walletUtils.summarizeSumPerCurrency(
                                walletData.expenseSumThisMonth,
                            ),
                            incomeSumThisMonth: this.walletUtils.summarizeSumPerCurrency(
                                walletData.incomeSumThisMonth,
                            ),
                        });
                    }),
                ),
            ),
        ),
        { initialValue: null },
    );

    /**
     * Az egyenleg nettó változása ebben a hónapban
     */
    protected balanceChangeThisMonth: Signal<WalletSummaryInterface[]> = computed(() => {
        const income = this.moneySumSummarizedPerCurrency()?.incomeSumThisMonth;
        const expense = this.moneySumSummarizedPerCurrency()?.expenseSumThisMonth;
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
