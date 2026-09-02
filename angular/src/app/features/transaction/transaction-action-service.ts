import { inject, Injectable, WritableSignal } from '@angular/core';
import { TransactionService } from './transaction-service';
import { _, TranslateService } from '@ngx-translate/core';
import { NewTransaction } from './interfaces';

// TODO ez törölhető, csak egy helyen vannak használva ezek a függvények és nem is kell több
@Injectable({ providedIn: 'root' })
export class TransactionActionService {
    private transactionService = inject(TransactionService);
    private translateService = inject(TranslateService);

    confirmDeletion(): boolean {
        return confirm(this.translateService.instant(_('transaction.delete.confirm')));
    }

    /**
     * Kitöröl egy tranzakciót
     *
     * @param id                Tranzakció azonosítója
     * @param isDisabled        egy signal, ami a formot disabled állapotra állítja
     * @param refreshCallback   mentés után az adatokat újratöltő függvény
     */
    deleteTransaction(
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
    saveTransaction(
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
