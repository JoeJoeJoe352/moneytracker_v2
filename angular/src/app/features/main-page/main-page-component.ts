import { Component, inject, OnInit, signal } from '@angular/core';
import TransactionListComponent from '../transaction/transaction-list-component';
import { TransactionModalComponent } from '../transaction/transaction-modal';
import { NewTransaction, TransactionDataFromBackend } from '../transaction/interfaces';
import { TransactionService } from '../transaction/transaction-service';
import { DecimalPipe } from '@angular/common';
import { _, TranslateService } from '@ngx-translate/core';
import { forkJoin, Observable } from 'rxjs';

@Component({
    selector: 'app-main-page-component',
    templateUrl: './main-page-component.html',
    styleUrl: './main-page-component.scss',
    standalone: true,
    imports: [TransactionModalComponent, TransactionListComponent, DecimalPipe],
})
export class MainPage implements OnInit {
    private transactionService = inject(TransactionService);
    private translateService = inject(TranslateService);

    // Tranzakció lista adatok és betöltési állapot
    protected transactionListData = signal<TransactionDataFromBackend[]>([]);
    protected isTransactionListLoaded = signal(false);
    // Tranzakció adatai, amit szerkeszteni szeretnénk. Adatok és betöltési állapot
    protected transactionData = signal<TransactionDataFromBackend | null>(null);
    protected isTransactionDataLoading = signal(false);

    protected moneySum = signal<number | null>(null);

    protected isTransactionFormDisabled = signal(false);

    ngOnInit() {
        this.loadMoneyData();
    }

    /**
     * Tranzakció létrehozó modal bezárása
     */
    protected isNewTransactionModalOpen = signal(false);

    /**
     * Tranzakció létrehozó modal felnyitása
     */
    protected openTransactionModal(): void {
        this.isNewTransactionModalOpen.set(true);
    }

    /**
     * Tranzakció létrehozó modal becsukása
     */
    protected closeTransactionModal(): void {
        this.transactionData.set(null);
        this.isNewTransactionModalOpen.set(false);
    }

    /**
     * Tranzakció létrehozó modal bezárása, ha változott az adat
     */
    protected refreshAfterSave(): void {
        this.loadMoneyData();
        this.closeTransactionModal();
    }

    /**
     * Feldob egy confirmot, hogy biztosan törölni szeretné-e a user a tranzakciót
     */
    popupDeletionConfirm(transactionId: number): void {
        if (confirm(this.translateService.instant(_('transaction.delete.confirm')))) {
            this.deleteTransaction(transactionId);
        }
    }

    /**
     * Tranzakció törlése a főoldalon
     */
    protected deleteTransaction(transactionId: number): void {
        this.runWithFormDisabled(this.transactionService.deleteTransaction(transactionId), () =>
            this.refreshAfterSave(),
        );
    }

    /**
     * Elment egy tranzakció adatait
     */
    saveTransaction(payload: NewTransaction): void {
        const transaction = this.transactionData();
        const obs =
            transaction !== null
                ? this.transactionService.updateTransaction(payload, transaction.id)
                : this.transactionService.saveTransaction(payload);

        this.runWithFormDisabled(obs, () => this.refreshAfterSave());
    }

    /**
     * Futtat egy api hívást és siker esetén meghív egy függvényt
     *
     * @param observable pl.: egy api hívás, ami observable-t ad vissza
     * @param onSuccess függvény, ami siker esetén lefut
     */
    private runWithFormDisabled<T>(observable: Observable<T>, onSuccess: () => void) {
        this.isTransactionFormDisabled.set(true);

        observable.subscribe({
            next: () => {
                this.isTransactionFormDisabled.set(false);
                onSuccess();
            },
            error: (response) => {
                console.error(
                    this.translateService.instant(_('transaction.delete.error')),
                    response,
                );
                this.isTransactionFormDisabled.set(false);
            },
        });
    }

    /**
     * Újratölti a pénzhez tartozó adatokat a főoldalon
     */
    private loadMoneyData(): void {
        forkJoin({
            list: this.transactionService.getLastTransactions(),
            sum: this.transactionService.getMoneySum(),
        }).subscribe({
            next: ({ list, sum }) => {
                this.transactionListData.set(list);
                this.moneySum.set(sum);
                this.isTransactionListLoaded.set(true);
            },
            error: () => {
                this.isTransactionListLoaded.set(false);
            },
        });
    }

    /**
     * Adott tranzakció letöltése a backendről, a szerkesztő formnak
     */
    protected loadTransaction(transactionId: number): void {
        this.isTransactionDataLoading.set(true);
        this.openTransactionModal();
        this.transactionService.getTransactionById(transactionId).subscribe({
            next: (response) => {
                this.isTransactionDataLoading.set(false);
                this.transactionData.set(response);
            },
            error: (response) => {
                console.error('unknown error during data loading!', response);
                this.isTransactionDataLoading.set(false);
            },
        });
    }
}
