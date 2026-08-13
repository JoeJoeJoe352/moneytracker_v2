import { Component, inject, Signal, signal, WritableSignal } from '@angular/core';
import TransactionListComponent from '../transaction/transaction-list-component';
import { TransactionModalComponent } from '../transaction/transaction-modal';
import { TransactionService } from '../transaction/transaction-service';
import { DecimalPipe } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, switchMap, tap } from 'rxjs';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { TransactionModalStateService } from '../transaction/transaction-modal-state-service';

@Component({
    selector: 'app-main-page-component',
    templateUrl: './main-page-component.html',
    styleUrl: './main-page-component.scss',
    standalone: true,
    imports: [TransactionModalComponent, TransactionListComponent, DecimalPipe, TranslatePipe],
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
    private reloadTransactionListTrigger = signal(0);

    /**
     * Töltődik-e jelenleg a tranzakciós lista
     */
    protected isTransactionListLoading = signal(true);
    /**
     * Összes pénz töltődik-e
     */
    protected isMoneySumLoading = signal(true);

    constructor() {
        // Mentés/törlés után újratöltjük a listát és az összesítést
        this.modal.changed.subscribe(() =>
            this.reloadTransactionListTrigger.update((value) => value + 1),
        );
    }

    /**
     * Tranzakciós lista adatok
     */
    protected transactionListData = this.reloadableSignal(
        this.reloadTransactionListTrigger,
        () => this.transactionService.getLastTransactions(),
        [],
        this.isTransactionListLoading,
    );

    /**
     * Felhasználó összes pénze.
     */
    protected moneySum = this.reloadableSignal(
        this.reloadTransactionListTrigger,
        () => this.transactionService.getMoneySum(),
        null,
        this.isMoneySumLoading,
    );

    /**
     * Egy trigger jelre újratölti az adatot, és a betöltés alatt/után beállítja a megadott loading jelzőt.
     * A transactionListData és moneySum azonos szerkezetű (trigger -> loading true -> lekérés -> loading false)
     * betöltési logikáját fogja össze, hogy ne kelljen kétszer leírni.
     */
    private reloadableSignal<T>(
        trigger: Signal<unknown>,
        load: () => Observable<T>,
        initialValue: T,
        loading: WritableSignal<boolean>,
    ): Signal<T> {
        return toSignal(
            toObservable(trigger).pipe(
                tap(() => loading.set(true)),
                switchMap(() => load().pipe(tap(() => loading.set(false)))),
            ),
            { initialValue },
        );
    }
}
