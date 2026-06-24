import { Component, inject, signal } from "@angular/core";
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
export class MainPage {
    private transactionService = inject(TransactionService);
    
    protected transaction = signal<Transaction|null>(null);
    protected isLoading = signal(false);

    /**
     * Tranzakció létrehozó modal nyitva van-e
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
     * Tranzakció lekérése
     * 
     * @param transactionId 
     */
    protected loadTransaction(transactionId: number): void {
        this.isLoading.set(true)
        this.transactionService.getTransactionById(transactionId).subscribe({
            next: (response) => {
                this.isLoading.set(false);
                this.transaction.set(response);
                this.openTransactionModal();
            },
            error: (response) => {
                console.error("unknown error during data loading!", response);
                this.isLoading.set(false);
            },
        })
    }
}