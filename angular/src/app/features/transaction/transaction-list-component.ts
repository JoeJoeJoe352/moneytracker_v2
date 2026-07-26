import { Component, EventEmitter, Input, Output } from '@angular/core';
import TransactionCardComponent from './transaction-card-component';
import { TransactionDataFromBackend } from './interfaces';

@Component({
    selector: 'app-transaction-list',
    template: `
        @for (transaction of transactions; track $index) {
            <app-transaction-card
                [transaction]="transaction"
                (click)="editTransaction.emit({ transactionId: transaction.id })"
            />
        }
    `,
    imports: [TransactionCardComponent],
})
export default class TransactionListComponent {
    /**
     * Megjelenítendő tranzakciók listája
     */
    @Input({ required: true }) transactions!: TransactionDataFromBackend[];

    /**
     * Ha user módosítani szeretne egy tranzakciót
     */
    @Output() editTransaction = new EventEmitter<{ transactionId: number }>();
}
