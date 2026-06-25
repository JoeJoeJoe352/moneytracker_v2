import { Component, inject, OnInit, signal } from "@angular/core";
import MoneySumComponent from "../transaction/money-sum-component";
import TransactionListComponent from "../transaction/transaction-list-component";
import { TransactionModalComponent } from "../transaction/transaction-modal";
import { Transaction } from "../transaction/interfaces";
import { TransactionService } from "../transaction/transaction-service";

@Component({
    selector: "app-main-page-component",
    templateUrl: "./main-page-component.html",
    styleUrl: "./main-page-component.scss",
    standalone: true,
    imports: [TransactionModalComponent, MoneySumComponent, TransactionListComponent],
})
export class MainPage implements OnInit{
    private transactionService = inject(TransactionService);

    // Tranzakció lista adatok és betöltési állapot
    protected transactionListData = signal<Transaction[]>([])
    protected isTransactionListLoaded = signal(false)
    // Tranzakció adatai, amit szerkeszteni szeretnénk. Adatok és betöltési állapot
    protected transactionData = signal<Transaction|null>(null);
    protected isTransactionDataLoading = signal(false);

    ngOnInit() {
        this.loadTransactionList()
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
    protected handleMondalDataChange(): void {
        this.loadTransactionList()
        this.closeTransactionModal();
    }

    /**
     * Tranzakciók letöltése a backendről, a kártyás listához
     */
    loadTransactionList(): void {
        this.transactionService.getLastTransactions().subscribe({
            next: (response) => {
                this.isTransactionListLoaded.set(true);
                this.transactionListData.set(response)
            },
            error: (response) => {
                console.error("unknown error during transaction creation!", response);
                this.isTransactionListLoaded.set(false);
            },
        })
    }

    /**
     * Adott tranzakció letöltése a backendről, a szerkesztő formnak
     */
    protected loadTransaction(transactionId: number): void {
        this.isTransactionDataLoading.set(true)
        this.transactionService.getTransactionById(transactionId).subscribe({
            next: (response) => {
                this.isTransactionDataLoading.set(false);
                this.transactionData.set(response);
                this.openTransactionModal();
            },
            error: (response) => {
                console.error("unknown error during data loading!", response);
                this.isTransactionDataLoading.set(false);
            },
        })
    }
}