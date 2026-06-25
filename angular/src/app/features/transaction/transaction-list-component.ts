import { Component, EventEmitter, Input, Output } from "@angular/core";
import TransactionCardComponent from "./transaction-card-component";
import { Transaction } from "./interfaces";

@Component({
    selector: "app-transaction-list",
    template: `
    @for (transaction of transactions; track $index) {
        <app-transaction-card 
            [transaction]="transaction" 
            (click)="editTransaction.emit({transactionId: transaction.id})"
        />
    }
    `,
    imports: [TransactionCardComponent]
})
export default class TransactionListComponent {
    @Input({ required: true }) transactions!: Transaction[];

    @Output() editTransaction = new EventEmitter<{transactionId: number}>();
}
