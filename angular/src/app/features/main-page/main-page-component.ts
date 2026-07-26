import { Component, computed, inject, signal } from '@angular/core';
import TransactionListComponent from '../transaction/transaction-list-component';
import { TransactionModalComponent } from '../transaction/transaction-modal';
import { NewTransaction } from '../transaction/interfaces';
import { TransactionService } from '../transaction/transaction-service';
import { DecimalPipe } from '@angular/common';
import { _, TranslatePipe, TranslateService } from '@ngx-translate/core';
import { of, switchMap, tap } from 'rxjs';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';

@Component({
    selector: 'app-main-page-component',
    templateUrl: './main-page-component.html',
    styleUrl: './main-page-component.scss',
    standalone: true,
    imports: [TransactionModalComponent, TransactionListComponent, DecimalPipe, TranslatePipe],
})
export class MainPage {
    private transactionService = inject(TransactionService);
    private translateService = inject(TranslateService);

    /**
     * Töltődik-e jelenleg a tranzakciós lista
     */
    protected isTransactionListLoading = signal(true);
    /**
     * Összes pénz töltődik-e
     */
    protected isMoneySumLoading = signal(true);
    /**
     * Kiválasztott tranzakció azonosítója.
     * Ha ez változik, akkor le fog futni a tranzakció betöltés is (transactionData)
     */
    protected selectedTransactionIdTrigger = signal<number | null>(null);
    /**
     * Tranzakciós form írható-e
     */
    protected isTransactionFormDisabled = signal(false);
    /**
     * Tranzakció létrehozó modal bezárása
     */
    protected isTransactionModalOpen = signal(false);
    /**
     * Újra kell-e tölteni az adatokat
     * Ha ez változik, akkor újra fogja tölteni a listát
     * Azért számot növelünk és nem boolean érték, mert ha gyorsan, többször hívódik egymás után, akkor kétszer true-ra állítódik az érték és az nem vált ki új letöltés eventet
     */
    protected reloadTransactionMoneyDataTrigger = signal(0);

    /**
     *  Betöltés alatt van-e a tranzakció
     */
    protected isTransactionDataLoading = computed(
        () => this.selectedTransactionIdTrigger() !== null && this.transactionData() === null,
    );

    /**
     * Tranzakciós lista adatok
     */
    protected transactionListData = toSignal(
        toObservable(this.reloadTransactionMoneyDataTrigger).pipe(
            tap(() => this.isTransactionListLoading.set(true)),
            switchMap(() =>
                this.transactionService.getLastTransactions().pipe(
                    tap(() => {
                        this.isTransactionListLoading.set(false);
                    }),
                ),
            ),
        ),
        {
            initialValue: [],
        },
    );
    /**
     * Felhasználó összes pénze.
     */
    protected moneySum = toSignal(
        toObservable(this.reloadTransactionMoneyDataTrigger).pipe(
            tap(() => this.isMoneySumLoading.set(true)),
            switchMap(() =>
                this.transactionService.getMoneySum().pipe(
                    tap(() => {
                        this.isMoneySumLoading.set(false);
                    }),
                ),
            ),
        ),
        { initialValue: null },
    );
    /**
     * Kiválasztott tranzakció adatai
     */
    protected transactionData = toSignal(
        toObservable(this.selectedTransactionIdTrigger).pipe(
            switchMap((id) =>
                id === null ? of(null) : this.transactionService.getTransactionById(id),
            ),
        ),
        { initialValue: null },
    );

    /**
     * Tranzakció létrehozó modal felnyitása
     */
    protected openTransactionModal(): void {
        this.isTransactionModalOpen.set(true);
    }

    /**
     * Tranzakció létrehozó modal becsukása
     */
    protected closeTransactionModal(): void {
        this.selectedTransactionIdTrigger.set(null);
        this.isTransactionModalOpen.set(false);
    }

    /**
     * Tranzakció betöltése szerkesztéshez
     */
    protected loadTransaction(id: number): void {
        this.selectedTransactionIdTrigger.set(id);
        this.openTransactionModal();
    }

    /**
     * Feldob egy confirmot, hogy biztosan törölni szeretné-e a user a tranzakciót
     */
    protected popupDeletionConfirm(transactionId: number): void {
        if (confirm(this.translateService.instant(_('transaction.delete.confirm')))) {
            this.deleteTransaction(transactionId);
        }
    }

    /**
     * Tranzakció törlése a főoldalon
     */
    protected deleteTransaction(transactionId: number): void {
        this.isTransactionFormDisabled.set(true);
        this.transactionService.deleteTransaction(transactionId).subscribe({
            next: () => {
                this.isTransactionFormDisabled.set(false);
                this.handleModalDataChange();
            },
            error: () => this.isTransactionFormDisabled.set(false),
        });
    }

    /**
     * Elment egy tranzakció adatait
     */
    protected saveTransaction(payload: NewTransaction): void {
        this.isTransactionFormDisabled.set(true);

        const transaction = this.transactionData();
        const observable = transaction
            ? this.transactionService.updateTransaction(payload, transaction.id)
            : this.transactionService.saveTransaction(payload);

        observable.subscribe({
            next: () => {
                this.isTransactionFormDisabled.set(false);
                this.handleModalDataChange();
            },
            error: () => this.isTransactionFormDisabled.set(false),
        });
    }

    /**
     * Modal adatváltozás lekezelése
     */
    private handleModalDataChange(): void {
        this.reloadTransactionMoneyDataTrigger.update((value) => value + 1);
        this.closeTransactionModal();
    }
}
