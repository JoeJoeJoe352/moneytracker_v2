import { Component } from '@angular/core';
import { TransactionsListComponent } from './transactions-component';

@Component({
    selector: 'app-transaction-page-component',
    template: '<app-transactions-list-component [isHistoryMode]="true" [needSearchField]="true"/>',
    imports: [TransactionsListComponent],
})
export class TransactionsPageComponent {}
