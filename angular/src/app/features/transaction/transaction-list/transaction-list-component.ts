import { Component, computed, input, output } from '@angular/core';
import { TransactionListElementData } from '../interfaces';
import { TranslatePipe } from '@ngx-translate/core';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import TransactionCardComponent from './transaction-card-component';
import { RESOURCE_STATUS_LOADING, RESOURCE_STATUS_RELOADING } from '../../../shared/constants';

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
    public listStatus = input.required<string>();
    /**
     * Megjelenítendő tranzakciók listája
     */
    public transactions = input.required<TransactionListElementData[]>();
    /**
     * Ha user módosítani szeretne egy tranzakciót
     */
    public editTransaction = output<{ transactionId: number }>();

    /**
     * Kezdő betöltődés alatt van-e a lista
     */
    protected isTransactionListLoading = computed(() => {
        return this.listStatus() === RESOURCE_STATUS_LOADING;
    });

    /**
     * Újratöltődés alatt van-e a lista
     */
    protected isTransactionListReLoading = computed(() => {
        return this.listStatus() === RESOURCE_STATUS_RELOADING;
    });

    /**
     * Modal felnyitás
     */
    protected openModalEmit(transactionId: number) {
        if (this.isTransactionListReLoading()) {
            return;
        }
        this.editTransaction.emit({ transactionId: transactionId });
    }
}
