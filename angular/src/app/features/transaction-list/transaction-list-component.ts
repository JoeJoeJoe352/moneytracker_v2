import { Component, EventEmitter, Input, Output } from '@angular/core';
import TransactionCardComponent from '../transaction-list/transaction-card-component';
import { TransactionListElementData } from './interfaces';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'app-transaction-list',
    templateUrl: './transaction-list-component.html',
    styleUrl: './transaction-list-component.scss',
    imports: [TransactionCardComponent, TranslatePipe],
})
export default class TransactionListComponent {
    /**
     * Lista címe
     */
    @Input() title = null as string | null;
    /**
     * Töltődik-e a lista jelenleg?
     */
    @Input({ required: true }) isTransactionListLoading!: boolean;
    /**
     * Megjelenítendő tranzakciók listája
     */
    @Input({ required: true }) transactions!: TransactionListElementData[];

    /**
     * Ha user módosítani szeretne egy tranzakciót
     */
    @Output() editTransaction = new EventEmitter<{ transactionId: number }>();
}
