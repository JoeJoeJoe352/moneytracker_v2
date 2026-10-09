import { Component, inject, output } from '@angular/core';
import { WalletSyncFormInputInterface, WalletSyncData } from './interfaces';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { TranslatePipe } from '@ngx-translate/core';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { DialogCloseButton } from '@app/shared/components/mat-modal-close';
import { MatIconModule } from '@angular/material/icon';

@Component({
    selector: 'app-wallet-sync-form-component',
    templateUrl: './wallet-sync-form-component.html',
    imports: [
        MatInputModule,
        MatButtonModule,
        MatIconModule,
        DialogCloseButton,
        MatFormFieldModule,
        ReactiveFormsModule,
        MatDialogModule,
        TranslatePipe,
    ],
})
export class WalletSyncFormComponent {
    private readonly fb = inject(FormBuilder);

    public data = inject<WalletSyncFormInputInterface>(MAT_DIALOG_DATA);
    /**
     * A user elküldte a formot
     */
    public syncFormSended = output<WalletSyncData>();
    /**
     * Form adatok
     */
    protected syncForm = this.fb.nonNullable.group({
        currentBalance: this.fb.control<number | null>(null, [
            Validators.required,
            Validators.min(0),
        ]),
    });

    /**
     * Form validálás és elküldése
     */
    protected submitForm(): void {
        const balanceFieldValue = this.syncForm.controls.currentBalance.value;

        if (!this.syncForm.valid || balanceFieldValue == null) {
            this.syncForm.markAllAsTouched();
            return;
        }

        this.syncFormSended.emit({
            walletId: this.data.wallet.id,
            currentBalance: balanceFieldValue,
        });
    }
}
