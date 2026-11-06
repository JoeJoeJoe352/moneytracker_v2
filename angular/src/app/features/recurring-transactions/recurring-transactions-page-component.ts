import { Component, computed, inject, resource } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { RecurringTransactionService } from './recurring-transactions-service';
import { firstValueFrom } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { _, TranslatePipe } from '@ngx-translate/core';
import { RESOURCE_STATUS_LOADING } from '@shared/constants';
import { NotificationService } from '@shared/services/notification-service';
import { RecurringTransactionResponseInterface } from './interfaces';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { RecurringTransactionModalComponent } from './recurring-transaction-modal';
import { RecurringTransactionRowComponent } from './recurring-transaction-row-component';
import { RecurringTransactionDoughnutComponent } from '../statistics/recurring-transaction-doughnut-chart-component';

const LIST_URL = '/recurring-transactions';

@Component({
    selector: 'app-recurring-transactions-page-component',
    templateUrl: './recurring-transactions-page-component.html',
    styleUrl: './recurring-transactions-page-component.scss',
    imports: [
        MatCardModule,
        MatProgressSpinner,
        MatButtonModule,
        TranslatePipe,
        RecurringTransactionRowComponent,
        RecurringTransactionDoughnutComponent
    ],
})
export class RecurringTransactionsPageComponent {
    private readonly service = inject(RecurringTransactionService);
    private readonly dialog = inject(MatDialog);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly notification = inject(NotificationService);

    constructor() {
        // /recurring-transactions/:id esetén a megadott ismétlődő tranzakció modalja nyílik fel
        this.route.params.pipe(takeUntilDestroyed()).subscribe((params) => {
            if (params['id'] === undefined) {
                return;
            }
            const idFromParam = Number(params['id']);
            if (this.isValidId(idFromParam)) {
                void this.openModalById(idFromParam);
            } else {
                console.error('Invalid recurring transaction id in param: ' + params['id']);
                void this.navigateToList();
            }
        });
    }

    /**
     * Ismétlődő tranzakciók, lista elemek
     */
    protected recurringTransactionListResource = resource({
        defaultValue: [],
        loader: async () => {
            return await firstValueFrom(this.service.listRecurringTransactions());
        },
    });

    /**
     * Ismétlődő tranzakciók, statisztikai elemek
     */
    protected recurringTransactionStatResource = resource({
        defaultValue: [],
        loader: async () => {
            return await firstValueFrom(this.service.recurringTransactionsStat());
        },
    });

    /**
     * Kezdő betöltődés alatt van-e a lista
     */
    protected isListLoading = computed(() => {
        return this.recurringTransactionListResource.status() === RESOURCE_STATUS_LOADING;
    });

    /**
     * Létrehozó/szerkesztő modal megnyitása, sikeres mentés után újratölti a listát.
     * Bezáráskor kiveszi az id-t az URL-ből, ha URL alapján nyílt meg
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
                if (this.route.snapshot.params['id'] !== undefined) {
                    void this.navigateToList();
                }
            });
    }

    /**
     * Lekéri az adott id-jú ismétlődő tranzakciót, és megnyitja a modalját. Ha nem található
     * (pl. más useré, vagy törölték), értesítést mutat, és visszanavigál a listára
     */
    private async openModalById(id: number): Promise<void> {
        try {
            const recurringTransaction = await firstValueFrom(
                this.service.getRecurringTransactionById(id),
            );
            this.openModal(recurringTransaction);
        } catch (err) {
            if (err instanceof HttpErrorResponse && err.status === 404) {
                this.notification.show(_('recurring_transaction.not_found'));
            } else {
                console.error('Problem with loading the recurring transaction', err);
                this.notification.showGeneralError();
            }
            void this.navigateToList();
        }
    }

    /**
     * Ismétlődő tranzakció id validálás (pl. kézzel írt URL esetén)
     */
    private isValidId(id: number): boolean {
        return Number.isInteger(id) && id > 0;
    }

    /**
     * Visszanavigál a listára (az id nélküli URL-re), a böngésző előzményeibe nem kerül be új bejegyzés
     */
    private navigateToList(): Promise<boolean> {
        return this.router.navigate([LIST_URL], { replaceUrl: true });
    }
}
