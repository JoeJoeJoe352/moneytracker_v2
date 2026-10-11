import { Component, ChangeDetectionStrategy } from '@angular/core';
import { TransactionsListComponent } from './transactions-component';

@Component({
    selector: 'app-transaction-page-component',
    template: '<app-transactions-list-component [isHistoryMode]="true" [needSearchField]="true"/>',
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [TransactionsListComponent],
})
export class TransactionsPageComponent {}
