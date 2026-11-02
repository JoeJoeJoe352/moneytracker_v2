import { Component, computed, inject, resource } from '@angular/core';
import { RecurringTransactionService } from './recurring-transactions-service';
import { firstValueFrom } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { TranslatePipe } from '@ngx-translate/core';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format-pipe';
import { RESOURCE_STATUS_LOADING } from '@shared/constants';
import { RecurringTransactionStatusEnum, TransactionTypeEnum } from '@shared/enums';
import { RecurringTransactionResponseInterface } from './interfaces';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { RecurringTransactionModalComponent } from './recurring-transaction-modal';

@Component({
    selector: 'app-recurring-transactions-page-component',
    templateUrl: './recurring-transactions-page-component.html',
    styleUrl: './recurring-transactions-page-component.scss',
    imports: [
        MatCardModule,
        MatIcon,
        MatProgressSpinner,
        MatButtonModule,
        TranslatePipe,
        CurrencyFormatPipe,
    ],
})
export class RecurringTransactionsPageComponent {
    private readonly service = inject(RecurringTransactionService);
    private readonly dialog = inject(MatDialog);

    protected readonly TransactionTypeEnum = TransactionTypeEnum;
    protected readonly RecurringTransactionStatusEnum = RecurringTransactionStatusEnum;

    protected recurringTransactionListResource = resource({
        defaultValue: [],
        loader: async () => {
            return await firstValueFrom(this.service.listRecurringTransactions());
        },
    });

    /**
     * Kezdő betöltődés alatt van-e a lista
     */
    protected isListLoading = computed(() => {
        return this.recurringTransactionListResource.status() === RESOURCE_STATUS_LOADING;
    });

    /**
     * A rendszeres tranzakció kategóriái vesszővel elválasztva felsorolva
     */
    protected categoryNames(transaction: RecurringTransactionResponseInterface): string {
        return transaction.categories.map((category) => category.name).join(', ');
    }

    /**
     * Létrehozó/szerkesztő modal megnyitása, sikeres mentés után újratölti a listát
     *
     * @param recurringTransaction Szerkesztendő ismétlődő tranzakció, null esetén új létrehozása
     */
    protected openModal(recurringTransaction: RecurringTransactionResponseInterface | null = null) {
        RecurringTransactionModalComponent.open(this.dialog, { recurringTransaction })
            .afterClosed()
            .subscribe((saved) => {
                if (saved) {
                    this.recurringTransactionListResource.reload();
                }
            });
    }
}
