import { Component, computed, inject, resource, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { RecurringTransactionService } from './recurring-transactions-service';
import { filter, firstValueFrom, switchMap, tap } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatProgressBar } from '@angular/material/progress-bar';
import { _, TranslatePipe, TranslateService } from '@ngx-translate/core';
import { RESOURCE_STATUS_LOADING } from '@shared/constants';
import { NotificationService } from '@shared/services/notification-service';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog-component';
import {
    RecurringTransactionResponseInterface,
    RecurringTransactionSaveRequestInterface,
} from './interfaces';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { RecurringTransactionModalComponent } from './recurring-transaction-modal';
import { RecurringTransactionRowComponent } from './recurring-transaction-row-component';
import { RecurringTransactionChartComponent } from '../statistics/recurring-transaction-chart-component';

const LIST_URL = '/recurring-transactions';

type RecurringTransactionModalRef = MatDialogRef<RecurringTransactionModalComponent, boolean>;

@Component({
    selector: 'app-recurring-transactions-page-component',
    templateUrl: './recurring-transactions-page-component.html',
    styleUrl: './recurring-transactions-page-component.scss',
    imports: [
        MatCardModule,
        MatProgressSpinner,
        MatProgressBar,
        MatButtonModule,
        TranslatePipe,
        RecurringTransactionRowComponent,
        RecurringTransactionChartComponent,
    ],
})
export class RecurringTransactionsPageComponent {
    private readonly service = inject(RecurringTransactionService);
    private readonly dialog = inject(MatDialog);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly notification = inject(NotificationService);
    private readonly translateService = inject(TranslateService);

    /**
     * Mentés/leállítás/törlés folyamatban van-e, addig a modal formja le van tiltva
     */
    private readonly isSaving = signal(false);

    /**
     * A szerkesztő modal adatainak lekérése folyamatban van-e. Ezalatt töltésjelző látszik,
     * és újabb modal nem nyitható (pl. dupla kattintásnál)
     */
    protected readonly isModalLoading = signal(false);

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
     * Létrehozó/szerkesztő modal megnyitása. 
     *
     * @param recurringTransactionId Szerkesztendő ismétlődő tranzakció id. Null esetén új létrehozása
     */
    protected openModal(recurringTransactionId: number | null = null) {
        if (this.isModalLoading()) {
            return;
        }
        if (recurringTransactionId === null) {
            this.openModalFor(null);
            return;
        }
        void this.openModalById(recurringTransactionId);
    }

    /**
     * Létrehozó/szerkesztő modal megnyitása, sikeres mentés után újratölti a listát.
     * Bezáráskor kiveszi az id-t az URL-ből, ha URL alapján nyílt meg
     *
     * @param recurringTransaction Szerkesztendő ismétlődő tranzakció, null esetén új létrehozása
     */
    private openModalFor(recurringTransaction: RecurringTransactionResponseInterface | null) {
        const dialogRef = RecurringTransactionModalComponent.open(this.dialog, {
            recurringTransaction,
            isSaving: this.isSaving.asReadonly(),
        });

        const modal = dialogRef.componentInstance;

        modal.saveRequested.subscribe((payload) =>
            this.save(dialogRef, recurringTransaction, payload),
        );
        modal.stopRequested.subscribe(() => this.stop(dialogRef, recurringTransaction));
        modal.deleteRequested.subscribe(() => this.delete(dialogRef, recurringTransaction));
        modal.intervalTypeChanged.subscribe(() => {
            ConfirmDialogComponent.open(this.dialog, {
                message: this.translateService.instant(
                    _('recurring_transaction.changed-interval'),
                ) as string,
                confirmLabel: this.translateService.instant(_('etc.close')) as string,
                hideCancelButton: true,
            });
        });

        dialogRef.afterClosed().subscribe((changed) => {
            if (changed) {
                this.recurringTransactionListResource.reload();
                // a diagram is ezeket az adatokat mutatja
                this.recurringTransactionStatResource.reload();
            }
            if (this.route.snapshot.params['id'] !== undefined) {
                void this.navigateToList();
            }
        });
    }

    /**
     * Elmenti (létrehozza vagy módosítja) az ismétlődő tranzakciót, siker esetén bezárja a modalt
     */
    private save(
        dialogRef: RecurringTransactionModalRef,
        recurringTransaction: RecurringTransactionResponseInterface | null,
        payload: RecurringTransactionSaveRequestInterface,
    ): void {
        const request =
            recurringTransaction === null
                ? this.service.createRecurringTransaction(payload)
                : this.service.updateRecurringTransaction(recurringTransaction.id, payload);

        this.isSaving.set(true);
        request.subscribe({
            next: () => {
                this.isSaving.set(false);
                this.notification.show(_('etc.save-success'));
                dialogRef.close(true);
            },
            error: () => this.isSaving.set(false),
        });
    }

    /**
     * Leállítja a szerkesztett ismétlődő tranzakciót, siker esetén bezárja a modalt
     */
    private stop(
        dialogRef: RecurringTransactionModalRef,
        recurringTransaction: RecurringTransactionResponseInterface | null,
    ): void {
        if (recurringTransaction === null) {
            console.error('Tried to stop a not yet created recurring transaction');
            return;
        }

        this.isSaving.set(true);
        this.service.stopRecurringTransaction(recurringTransaction.id).subscribe({
            next: () => {
                this.isSaving.set(false);
                this.notification.show(_('recurring_transaction.form.stop-success'));
                dialogRef.close(true);
            },
            error: () => this.isSaving.set(false),
        });
    }

    /**
     * Megerősítés után törli a szerkesztett ismétlődő tranzakciót, siker esetén bezárja a modalt
     */
    private delete(
        dialogRef: RecurringTransactionModalRef,
        recurringTransaction: RecurringTransactionResponseInterface | null,
    ): void {
        if (recurringTransaction === null) {
            console.error('Tried to delete a not yet created recurring transaction');
            return;
        }

        ConfirmDialogComponent.open(this.dialog, {
            message: this.translateService.instant(
                _('recurring_transaction.form.delete_confirm'),
            ) as string,
            danger: true,
        })
            .afterClosed()
            .pipe(
                filter((confirmed) => confirmed === true),
                tap(() => this.isSaving.set(true)),
                switchMap(() => this.service.deleteRecurringTransaction(recurringTransaction.id)),
            )
            .subscribe({
                next: () => {
                    this.isSaving.set(false);
                    this.notification.show(_('etc.delete-success'));
                    dialogRef.close(true);
                },
                error: () => this.isSaving.set(false),
            });
    }

    /**
     * Lekéri az adott id-jú ismétlődő tranzakciót, és megnyitja a modalját. Ha nem található
     * (pl. más useré, vagy törölték), értesítést mutat, és ha URL alapján nyílt volna meg,
     * visszanavigál a listára
     */
    private async openModalById(id: number): Promise<void> {
        if (this.isModalLoading()) {
            return;
        }
        this.isModalLoading.set(true);
        try {
            const recurringTransaction = await firstValueFrom(
                this.service.getRecurringTransactionById(id),
            );
            this.openModalFor(recurringTransaction);
        } catch (err) {
            if (err instanceof HttpErrorResponse && err.status === 404) {
                this.notification.show(_('recurring_transaction.not_found'));
            } else {
                console.error('Problem with loading the recurring transaction', err);
                this.notification.showGeneralError();
            }
            if (this.route.snapshot.params['id'] !== undefined) {
                void this.navigateToList();
            }
        } finally {
            this.isModalLoading.set(false);
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
