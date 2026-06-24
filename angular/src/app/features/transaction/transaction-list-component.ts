import { Component, inject, OnInit, signal } from "@angular/core";
import { TransactionService } from "./transaction-service";
import TransactionCardComponent from "./transaction-card-component";
import { Transaction } from "./interfaces";

@Component({
    selector: "app-transaction-list",
    template: `
    @if (isLoaded()) {
        @for (transaction of transactions(); track $index) {
            <app-transaction-card [transaction]="transaction"/>
        }
    } @else {
        <div class="text-center">
            <div class="spinner-border" role="status" ></div>
        </div>
    }
    `,
    imports: [TransactionCardComponent]
})
export default class TransactionListComponent implements OnInit {

    transactionService = inject(TransactionService)

    transactions = signal<null|Transaction[]>(null)
    isLoaded = signal(false)

    ngOnInit() {
        // Feliratkozunk az eseményre, hogy tudjuk mikor kell frissíteni a listát
        this.transactionService.refreshNeeded$.subscribe(() => {
            this.loadTransactions()
        })
        this.loadTransactions()
    }

    /**
     * Tranzakciók betöltése a listába
     */
    loadTransactions(): void {
        this.transactionService.getLastTransactions().subscribe({
            next: (response) => {
                this.isLoaded.set(true);
                this.transactions.set(response)
            },
            error: (response) => {
                console.error("unknown error during transaction creation!", response);
                this.isLoaded.set(false);
            },
        })
    }
}
