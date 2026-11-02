import { Component, computed, input, output } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format-pipe';
import { RecurringTransactionStatusEnum, TransactionTypeEnum } from '@shared/enums';
import { RecurringTransactionResponseInterface } from './interfaces';

@Component({
    selector: 'app-recurring-transaction-row-component',
    templateUrl: './recurring-transaction-row-component.html',
    styleUrl: './recurring-transaction-row-component.scss',
    imports: [MatIcon, TranslatePipe, CurrencyFormatPipe],
    host: {
        role: 'button',
        tabindex: '0',
        '[attr.aria-label]': 'transaction().name',
        '(click)': 'selected.emit()',
        '(keydown.enter)': 'selected.emit()',
        '(keydown.space)': '$event.preventDefault(); selected.emit()',
    },
})
export class RecurringTransactionRowComponent {
    /**
     * Megjelenítendő ismétlődő tranzakció
     */
    transaction = input.required<RecurringTransactionResponseInterface>();

    /**
     * Sor kiválasztása (kattintás, enter vagy space)
     */
    selected = output<void>();

    /**
     * Bevétel-e a tranzakció
     */
    protected isIncome = computed(() => {
        return this.transaction().transactionType === TransactionTypeEnum.INCOME;
    });

    /**
     * Aktív-e az ismétlődő tranzakció
     */
    protected isActive = computed(() => {
        return this.transaction().status === RecurringTransactionStatusEnum.ACTIVE;
    });

    /**
     * A rendszeres tranzakció kategóriái vesszővel elválasztva felsorolva
     */
    protected categoryNames = computed(() => {
        return this.transaction()
            .categories.map((category) => category.name)
            .join(', ');
    });
}
