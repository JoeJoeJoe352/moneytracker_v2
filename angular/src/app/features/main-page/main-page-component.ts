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
import { WalletSummaryInterface } from '../transaction/interfaces';

@Component({
    selector: 'app-main-page-component',
    templateUrl: './main-page-component.html',
    styleUrl: './main-page-component.scss',
    standalone: true,
    imports: [TransactionsListComponent, DecimalPipe, TranslatePipe, TransactionModalComponent],
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
    protected moneySumSummarizedPerCurrency: WritableSignal<WalletSummaryInterface[]> = signal([]);

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
                        this.moneySumSummarizedPerCurrency.set(
                            this.walletUtils.summarizeSumPerCurrency(walletData.moneySum),
                        );
                    }),
                ),
            ),
        ),
        { initialValue: null },
    );

    /**
     * Az összesített pénz összege szöveges formában valutánként
     */
    protected walletSumAsString: Signal<string[]> = computed(() =>
        this.moneySumSummarizedPerCurrency().map((walletData) => {
            const currencyText = this.walletUtils.getCurrencyTextForCurrencyCode(
                walletData.currencyCode,
            );

            return `${this.decimalPipe.transform(walletData.total, '4.0-5')} ${this.translateService.instant(currencyText)}`;
        }),
    );

    /**
     * Az egyenleg nettó változása ebben a hónapban
     */
    protected balanceChangeThisMonth: Signal<number | null> = computed(() => {
        const sum = this.moneySum();
        return sum ? sum.incomeSumThisMonth - sum.expenseSumThisMonth : null;
    });
}
