import { Component, inject, OnInit, signal } from '@angular/core';
import TransactionListComponent from '../transaction/transaction-list-component';
import { TransactionModalComponent } from '../transaction/transaction-modal';
import { NewTransaction, TransactionDataFromBackend } from '../transaction/interfaces';
import { TransactionService } from '../transaction/transaction-service';
import { DecimalPipe } from '@angular/common';
import { _, TranslateService } from '@ngx-translate/core';

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
    protected handleModalDataChange(): void {
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
        this.isTransactionFormDisabled.set(true);
        this.transactionService.deleteTransaction(transactionId).subscribe({
            next: () => {
                this.isTransactionFormDisabled.set(false);
                this.handleModalDataChange();
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
     * Tranzakció mentése a főoldalon
     */
    protected saveTransaction(payload: NewTransaction) {
        const transaction = this.transactionData()
        // Létezik a tranzakció, update!
        if (transaction !== null) {
            const transactionId = transaction.id;
            this.transactionService.updateTransaction(payload, transactionId).subscribe({
                next: () => {
                    this.isTransactionFormDisabled.set(false);
                    this.handleModalDataChange()
                },
                error: (response) => {
                    console.error(
                        this.translateService.instant(_('transaction.update.error')),
                        response,
                    );
                    this.isTransactionFormDisabled.set(false);
                },
            });
        } else {
            this.transactionService.saveTransaction(payload).subscribe({
                next: () => {
                    this.isTransactionFormDisabled.set(false);
                    this.handleModalDataChange()
                },
                error: (response) => {
                    console.error(
                        this.translateService.instant(_('transaction.create.error')),
                        response,
                    );
                    this.isTransactionFormDisabled.set(false);
                },
            });
        }
    }

    /**
     * Újratölti a pénzhez tartozó adatokat a főoldalon
     */
    private loadMoneyData(): void {
        this.loadTransactionList();
        this.loadMoneySum();
    }

    /**
     * Tranzakciók letöltése a backendről, a kártyás listához
     */
    loadTransactionList(): void {
        this.transactionService.getLastTransactions().subscribe({
            next: (response) => {
                this.isTransactionListLoaded.set(true);
                this.transactionListData.set(response);
            },
            error: (response) => {
                console.error('unknown error during transaction creation!', response);
                this.isTransactionListLoaded.set(false);
            },
        });
    }

    /**
     * Adott tranzakció letöltése a backendről, a szerkesztő formnak
     */
    protected loadTransaction(transactionId: number): void {
        this.isTransactionDataLoading.set(true);
        this.transactionService.getTransactionById(transactionId).subscribe({
            next: (response) => {
                this.isTransactionDataLoading.set(false);
                this.transactionData.set(response);
                this.openTransactionModal();
            },
            error: (response) => {
                console.error('unknown error during data loading!', response);
                this.isTransactionDataLoading.set(false);
            },
        });
    }

    /**
     * Betölti az összesített pénzét a usernek
     */
    protected loadMoneySum(): void {
        this.transactionService.getMoneySum().subscribe({
            next: (response) => {
                this.moneySum.set(response);
            },
            error: (response) => {
                console.error('unknown error during transaction creation!', response);
            },
        });
    }
}
