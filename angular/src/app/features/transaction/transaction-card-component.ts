import { Component, computed, Input } from '@angular/core';
import { TransactionDataFromBackend } from './interfaces';
import { TransactionTypeEnum } from './transaction-type-enum';
import { DecimalPipe } from '@angular/common';

@Component({
    selector: 'app-transaction-card',
    templateUrl: './transaction-card-component.html',
    styleUrl: './transaction-card-component.scss',
    imports: [DecimalPipe],
})
export default class TransactionCardComponent {
    /**
     * Megjelenítendő tranzakció adatai
     */
    @Input({ required: true }) transaction!: TransactionDataFromBackend;

    /**
     * Tranzakció típusa bevétel-e
     */
    protected isIncome = computed(
        () => this.transaction.transactionType == TransactionTypeEnum.INCOME,
    );
}
