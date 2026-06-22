import { Component, inject, OnInit, signal } from "@angular/core";
import { TransactionService } from "./transaction-service";
import TransactionCardComponent from "./transaction-card-component";
import { Transaction } from "./interfaces";

@Component({
    selector: "app-transaction-list",
    template: `
        @for (transaction of transactions(); track $index) {
            <app-transaction-card [transaction]="transaction"/>
        }
    `,
    imports: [TransactionCardComponent]
})
export default class TransactionListComponent implements OnInit{

    transactionService = inject(TransactionService)
    transactions = signal<null|Transaction[]>(null)
    isLoaded = signal(false)

    ngOnInit() {
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