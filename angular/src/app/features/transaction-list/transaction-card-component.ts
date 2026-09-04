import { Component, computed, Input, Signal } from '@angular/core';
import { TransactionListElementData } from './interfaces';
import { TransactionTypeEnum } from '../transaction/transaction-type-enum';
import { CurrencyFormatPipe } from '../../shared/pipes/currency-format-pipe';

@Component({
    selector: 'app-transaction-card',
    templateUrl: './transaction-card-component.html',
    styleUrl: './transaction-card-component.scss',
    imports: [CurrencyFormatPipe],
})
export default class TransactionCardComponent {
    /**
     * Megjelenítendő tranzakció adatai
     */
    @Input({ required: true }) transaction!: TransactionListElementData;

    /**
     * Tranzakció típusa bevétel-e
     */
    protected isIncome: Signal<boolean> = computed(
        () => this.transaction.transactionType == TransactionTypeEnum.INCOME,
    );

    /**
     * Tranzakció kategóriák listája
     */
    protected categoryList: Signal<Set<string>> = computed(() => {
        const categories: string[] = this.transaction.transactionDetails.flatMap(
            (detail) => detail.categories,
        );
        return new Set(categories);
    });

    /**
     * Kategóriák listája felsorolva, vesszővel elválasztva
     */
    protected categoriesAsString: Signal<string> = computed(() => {
        return Array.from(this.categoryList()).join(', ');
    });
}
