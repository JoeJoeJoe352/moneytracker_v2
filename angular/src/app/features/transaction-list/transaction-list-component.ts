import { Component, input, output } from '@angular/core';
import { TransactionListElementData } from './interfaces';
import { TranslatePipe } from '@ngx-translate/core';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import TransactionCardComponent from './transaction-card-component';

@Component({
    selector: 'app-transaction-list',
    templateUrl: './transaction-list-component.html',
    styleUrl: './transaction-list-component.scss',
    imports: [TransactionCardComponent, TranslatePipe, MatProgressSpinner, MatCardModule],
})
export default class TransactionListComponent {
    /**
     * Lista címe
     */
    public title = input<string | null>(null);
    /**
     * Töltődik-e a lista jelenleg?
     */
    public isTransactionListLoading = input.required<boolean>();
    /**
     * Megjelenítendő tranzakciók listája
     */
    public transactions = input.required<TransactionListElementData[]>();
    /**
     * Ha user módosítani szeretne egy tranzakciót
     */
    public editTransaction = output<{ transactionId: number }>();
}
