import { Component } from '@angular/core';
import { TransactionsListComponent } from '../transaction/transactions-component';

@Component({
    selector: 'app-transaction-page-component',
    template: '<app-transactions-list-component [isHistoryMode]="true" [needSearchField]="true"/>',
    standalone: true,
    imports: [TransactionsListComponent],
})
export class TransactionsPage {}
