import { Component, inject, resource, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import {
    MAT_DIALOG_DATA,
    MatDialog,
    MatDialogModule,
    MatDialogRef,
} from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { _, TranslatePipe, TranslateService } from '@ngx-translate/core';
import { filter, firstValueFrom } from 'rxjs';
import { DialogCloseButton } from '@shared/components/mat-modal-close';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog-component';
import { NotificationService } from '@shared/services/notification-service';
import { TransferTransactionFormComponent } from './transfer-transaction-form-component';
import { TransferTransactionService } from './transfer-transaction-service';
import { TransferTransactionFormValue, TransferTransactionResponse } from './interfaces';

export interface TransferTransactionModalInputInterface {
    /**
     * A szerkesztendő átvezetés egyik oldalának (tranzakciójának) id-ja, null esetén új átvezetés
     */
    transactionId: number | null;
}

/**
 * Átvezetés létrehozó/szerkesztő modal. A betöltést, mentést és törlést maga végzi, sikeres
 * mentés/törlés után true-val záródik, így a hívó újratöltheti a saját adatait
 */
@Component({
    selector: 'app-transfer-transaction-modal',
    templateUrl: './transfer-transaction-modal.html',
    imports: [
        TransferTransactionFormComponent,
        TranslatePipe,
        MatDialogModule,
        MatProgressSpinnerModule,
        DialogCloseButton,
    ],
})
export class TransferTransactionModalComponent {
    private readonly service = inject(TransferTransactionService);
    private readonly notification = inject(NotificationService);
    private readonly translateService = inject(TranslateService);
    private readonly dialog = inject(MatDialog);
    private readonly dialogRef =
        inject<MatDialogRef<TransferTransactionModalComponent, boolean>>(MatDialogRef);
    protected readonly data = inject<TransferTransactionModalInputInterface>(MAT_DIALOG_DATA);

    /**
     * Mentés/törlés folyamatban van-e, addig a form le van tiltva
     */
    protected readonly isSaving = signal(false);

    /**
     * A szerkesztendő átvezetés. Új átvezetésnél null, nem töltődik semmi
     */
    protected readonly transferResource = resource<
        TransferTransactionResponse | null,
        number | null
    >({
        defaultValue: null,
        params: () => this.data.transactionId,
        loader: async ({ params: transactionId }) => {
            if (transactionId === null) {
                return null;
            }
            try {
                return await firstValueFrom(
                    this.service.getTransferTransactionByTransactionId(transactionId),
                );
            } catch (err) {
                if (err instanceof HttpErrorResponse && err.status === 404) {
                    this.dialogRef.close();
                    this.notification.show(_('transaction.not-found'));
                    return null;
                }
                throw err;
            }
        },
    });

    /**
     * Modal megnyitása. Sikeres mentés/törlés esetén true-val tér vissza az afterClosed()
     */
    public static open(
        dialog: MatDialog,
        data: TransferTransactionModalInputInterface = { transactionId: null },
    ) {
        return dialog.open<
            TransferTransactionModalComponent,
            TransferTransactionModalInputInterface,
            boolean
        >(TransferTransactionModalComponent, {
            restoreFocus: true,
            width: '600px',
            data,
        });
    }

    /**
     * Elmenti (létrehozza vagy módosítja) az átvezetést, siker esetén bezárja a modalt
     */
    protected save(formValue: TransferTransactionFormValue): void {
        const transfer = this.transferResource.value();
        const request =
            transfer === null
                ? this.service.createTransferTransaction(formValue)
                : this.service.updateTransferTransaction(transfer.transferId, formValue);

        this.isSaving.set(true);
        request.subscribe({
            next: () => {
                this.isSaving.set(false);
                this.notification.show(_('etc.save-success'));
                this.dialogRef.close(true);
            },
            error: () => this.isSaving.set(false),
        });
    }

    /**
     * Megerősítés után törli az átvezetést (mindkét oldalát), siker esetén bezárja a modalt
     */
    protected confirmDelete(): void {
        const transfer = this.transferResource.value();
        if (transfer === null) {
            console.error('Tried to delete a not yet created transfer transaction');
            return;
        }

        ConfirmDialogComponent.open(this.dialog, {
            message: this.translateService.instant(
                _('transfer_transaction.delete.confirm'),
            ) as string,
            danger: true,
        })
            .afterClosed()
            .pipe(filter(Boolean))
            .subscribe(() => {
                this.isSaving.set(true);
                this.service.deleteTransferTransaction(transfer.transferId).subscribe({
                    next: () => {
                        this.isSaving.set(false);
                        this.notification.show(_('etc.delete-success'));
                        this.dialogRef.close(true);
                    },
                    error: () => this.isSaving.set(false),
                });
            });
    }
}
