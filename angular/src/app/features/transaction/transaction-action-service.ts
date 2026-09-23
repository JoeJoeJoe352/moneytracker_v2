import { inject, Injectable, WritableSignal } from '@angular/core';
import { TransactionService } from './transaction-service';
import { _, TranslateService } from '@ngx-translate/core';
import { NewTransaction } from './interfaces';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog-component';
import { MatDialog } from '@angular/material/dialog';
import { Observable, map } from 'rxjs';

// TODO ez törölhető, csak egy helyen vannak használva ezek a függvények és nem is kell több
@Injectable({ providedIn: 'root' })
export class TransactionActionService {
    private readonly transactionService = inject(TransactionService);
    private readonly translateService = inject(TranslateService);
    private readonly dialog = inject(MatDialog);

    /**
     * Törlés dialog feldobása
     */
    public confirmDeletion(): Observable<boolean> {
        return ConfirmDialogComponent.open(this.dialog, {
            message: this.translateService.instant(_('transaction.delete.confirm')),
            danger: true,
        })
            .afterClosed()
            .pipe(map((confirmed) => confirmed ?? false));
    }

    /**
     * Kitöröl egy tranzakciót
     *
     * @param id                Tranzakció azonosítója
     * @param isDisabled        egy signal, ami a formot disabled állapotra állítja
     * @param refreshCallback   mentés után az adatokat újratöltő függvény
     */
    public deleteTransaction(
        id: number,
        isDisabled: WritableSignal<boolean>,
        refreshCallback: () => void,
    ) {
        isDisabled.set(true);

        this.transactionService.deleteTransaction(id).subscribe({
            next: () => {
                isDisabled.set(false);
                refreshCallback();
            },
            error: () => isDisabled.set(false),
        });
    }

    /**
     * Elment egy tranzakciót
     *
     * @param payload           Tranzakciós adatok
     * @param transactionId     Tranzakció azonosítója
     * @param isDisabled        egy signal, ami a formot disabled állapotra állítja
     * @param refreshCallback   mentés után az adatokat újratöltő függvény
     */
    public saveTransaction(
        payload: NewTransaction,
        transactionId: number | null,
        isDisabled: WritableSignal<boolean>,
        refreshCallback: () => void,
    ) {
        isDisabled.set(true);

        const obs =
            transactionId !== null
                ? this.transactionService.updateTransaction(payload, transactionId)
                : this.transactionService.saveTransaction(payload);

        obs.subscribe({
            next: () => {
                isDisabled.set(false);
                refreshCallback();
            },
            error: () => isDisabled.set(false),
        });
    }
}
