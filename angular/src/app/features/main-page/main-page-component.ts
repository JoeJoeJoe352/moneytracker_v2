import { Component, computed, inject, Signal, signal } from '@angular/core';
import { TransactionService } from '../transaction/transaction-service';
import { DecimalPipe } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import { switchMap, tap } from 'rxjs';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { TransactionModalStateService } from '../transaction/transaction-modal-state-service';
import { TransactionsListComponent } from '../transaction/transactions-component';
import { TransactionModalComponent } from '../transaction/transaction-modal';

@Component({
    selector: 'app-main-page-component',
    templateUrl: './main-page-component.html',
    styleUrl: './main-page-component.scss',
    standalone: true,
    imports: [TransactionsListComponent, DecimalPipe, TranslatePipe, TransactionModalComponent],
    providers: [TransactionModalStateService],
})
export class MainPage {
    private transactionService = inject(TransactionService);
    protected modal = inject(TransactionModalStateService);

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
                this.transactionService
                    .getMoneySum()
                    .pipe(tap(() => this.isMoneySumLoading.set(false))),
            ),
        ),
        { initialValue: null },
    );

    /**
     * Az egyenleg nettó változása ebben a hónapban
     */
    protected balanceChangeThisMonth: Signal<number | null> = computed(() => {
        const sum = this.moneySum();
        return sum ? sum.incomeSumThisMonth - sum.expenseSumThisMonth : null;
    });
}
