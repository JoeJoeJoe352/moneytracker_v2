import { Component, computed, input, output } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { _, TranslatePipe } from '@ngx-translate/core';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format-pipe';
import {
    RecurrenceIntervalEnum,
    RecurringTransactionStatusEnum,
    TransactionTypeEnum,
} from '@shared/enums';
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
     * Megjelenjen-e a wallet neve (pl. a wallet saját modaljában felesleges)
     */
    showWalletName = input(true);

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

    /**
     * A státusz fordítási kulcsa. Új státusz esetén a never típus miatt fordítási
     * hibát kapunk, amíg nincs itt lekezelve
     */
    protected statusTranslateKey = computed(() => {
        const status = this.transaction().status;
        switch (status) {
            case RecurringTransactionStatusEnum.ACTIVE:
                return _('recurring_transaction.status.active');
            case RecurringTransactionStatusEnum.STOPPED:
                return _('recurring_transaction.status.stopped');
            case RecurringTransactionStatusEnum.DELETED:
                return _('recurring_transaction.status.deleted');
            default: {
                const unknownStatus: never = status;
                throw new Error(`Unknown status: ${String(unknownStatus)}`);
            }
        }
    });

    /**
     * Az ismétlődés gyakoriságának fordítási kulcsa
     */
    protected intervalTranslateKey = computed(() => {
        const interval = this.transaction().recurrenceInterval;
        switch (interval) {
            case RecurrenceIntervalEnum.WEEKLY:
                return _('recurring_transaction.interval.weekly');
            case RecurrenceIntervalEnum.MONTHLY:
                return _('recurring_transaction.interval.monthly');
            default: {
                const unknownInterval: never = interval;
                throw new Error(`Unknown interval: ${String(unknownInterval)}`);
            }
        }
    });
}
