import { Component, inject, OnInit, signal } from "@angular/core";
import MoneySumComponent from "../transaction/money-sum-component";
import TransactionListComponent from "../transaction/transaction-list-component";
import { TransactionModalComponent } from "../transaction/create-transaction-modal";
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
    protected transactionList = signal<Transaction[]>([])
    protected isTransactionListLoaded = signal(false)
    // Tranzakció adatai, amit szerkeszteni szeretnénk. Adatok és betöltési állapot
    protected transaction = signal<Transaction|null>(null);
    protected isTransactionDataLoading = signal(false);

    ngOnInit() {
        this.loadTransactionList()
    }

    /**
     * Tranzakciók betöltése a listába
     */
    loadTransactionList(): void {
        this.transactionService.getLastTransactions().subscribe({
            next: (response) => {
                this.isTransactionListLoaded.set(true);
                this.transactionList.set(response)
            },
            error: (response) => {
                console.error("unknown error during transaction creation!", response);
                this.isTransactionListLoaded.set(false);
            },
        })
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
     * Tranzakció lekérése
     * 
     * @param transactionId 
     */
    protected loadTransaction(transactionId: number): void {
        this.isTransactionDataLoading.set(true)
        this.transactionService.getTransactionById(transactionId).subscribe({
            next: (response) => {
                this.isTransactionDataLoading.set(false);
                this.transaction.set(response);
                this.openTransactionModal();
            },
            error: (response) => {
                console.error("unknown error during data loading!", response);
                this.isTransactionDataLoading.set(false);
            },
        })
    }
}