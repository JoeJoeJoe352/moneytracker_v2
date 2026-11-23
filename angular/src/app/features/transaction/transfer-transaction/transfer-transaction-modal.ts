import { Component, inject, signal } from '@angular/core';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { _, TranslatePipe } from '@ngx-translate/core';
import { DialogCloseButton } from '@shared/components/mat-modal-close';
import { NotificationService } from '@shared/services/notification-service';
import { TransferTransactionFormComponent } from './transfer-transaction-form-component';
import { TransferTransactionService } from './transfer-transaction-service';
import { TransferTransactionFormValue } from './interfaces';

/**
 * Átvezetés létrehozó modal. A mentést maga végzi, sikeres mentés után true-val
 * záródik, így a hívó újratöltheti a saját adatait
 */
@Component({
    selector: 'app-transfer-transaction-modal',
    templateUrl: './transfer-transaction-modal.html',
    imports: [TransferTransactionFormComponent, TranslatePipe, MatDialogModule, DialogCloseButton],
})
export class TransferTransactionModalComponent {
    private readonly service = inject(TransferTransactionService);
    private readonly notification = inject(NotificationService);
    private readonly dialogRef =
        inject<MatDialogRef<TransferTransactionModalComponent, boolean>>(MatDialogRef);

    /**
     * Mentés folyamatban van-e, addig a form le van tiltva
     */
    protected readonly isSaving = signal(false);

    /**
     * Modal megnyitása. Sikeres mentés esetén true-val tér vissza az afterClosed()
     */
    public static open(dialog: MatDialog) {
        return dialog.open<TransferTransactionModalComponent, void, boolean>(
            TransferTransactionModalComponent,
            {
                restoreFocus: true,
                width: '600px',
            },
        );
    }

    /**
     * Elmenti az átvezetést, siker esetén bezárja a modalt
     */
    protected save(formValue: TransferTransactionFormValue): void {
        this.isSaving.set(true);

        this.service.createTransferTransaction(formValue).subscribe({
            next: () => {
                this.isSaving.set(false);
                this.notification.show(_('etc.save-success'));
                this.dialogRef.close(true);
            },
            error: () => this.isSaving.set(false),
        });
    }
}
