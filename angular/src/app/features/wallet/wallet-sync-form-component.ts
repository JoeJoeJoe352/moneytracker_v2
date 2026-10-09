import { Component, computed, inject, output } from '@angular/core';
import { WalletSyncFormInputInterface, WalletSyncData } from './interfaces';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { _, TranslatePipe, TranslateService } from '@ngx-translate/core';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { DialogCloseButton } from '@app/shared/components/mat-modal-close';
import { MatIconModule } from '@angular/material/icon';
import { CurrencyFormatPipe } from '@app/shared/pipes/currency-format-pipe';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
    selector: 'app-wallet-sync-form-component',
    templateUrl: './wallet-sync-form-component.html',
    styleUrl: './wallet-sync-form-component.scss',
    imports: [
        MatInputModule,
        MatButtonModule,
        MatIconModule,
        DialogCloseButton,
        MatFormFieldModule,
        ReactiveFormsModule,
        MatDialogModule,
        TranslatePipe,
        CurrencyFormatPipe,
    ],
    providers: [CurrencyFormatPipe],
})
export class WalletSyncFormComponent {
    private readonly fb = inject(FormBuilder);
    private readonly translateService = inject(TranslateService);
    private readonly currencyFormatPipe = inject(CurrencyFormatPipe);

    public data = inject<WalletSyncFormInputInterface>(MAT_DIALOG_DATA);
    /**
     * A user elküldte a formot
     */
    public syncFormSended = output<WalletSyncData>();
    /**
     * Form adatok
     */
    protected syncForm = this.fb.nonNullable.group({
        currentBalance: this.fb.control<number | null>(null, [Validators.required]),
    });
    /**
     * Az egyenleg form elem signal-ja
     */
    private readonly currentBalance = toSignal(this.syncForm.controls.currentBalance.valueChanges, {
        initialValue: this.syncForm.controls.currentBalance.value,
    });

    /**
     * Form validálás és elküldése
     */
    protected submitForm(): void {
        const balanceFieldValue = this.currentBalance();

        if (!this.syncForm.valid || balanceFieldValue == null) {
            this.syncForm.markAllAsTouched();
            return;
        }

        this.syncFormSended.emit({
            walletId: this.data.wallet.id,
            currentBalance: balanceFieldValue,
        });
    }

    /**
     * Kigenerálja a szöveget, hogy mi lesz az eredménye a szinkronizációnak
     */
    protected resultText = computed(() => {
        const balanceDiff = this.balanceDifference();
        if (balanceDiff === null) {
            return '';
        }
        if (balanceDiff === 0) {
            return this.translateService.instant(_('wallet.sync.modal_no_result_description'));
        }
        const type =
            balanceDiff > 0
                ? this.translateService.instant(_('transaction.income'))
                : this.translateService.instant(_('transaction.expense'));

        return this.translateService.instant(_('wallet.sync.modal_result_description'), {
            type: type,
            value: this.currencyFormatPipe.transform(
                Math.abs(balanceDiff),
                this.data.wallet.currencyCode,
            ),
        });
    });

    /**
     * Megadja, hogy mekkora az egyenleg eltérés a user által megadott és a rendszerben tárolt között
     */
    protected balanceDifference = computed(() => {
        const currentBalanceLocal = this.currentBalance();
        if (currentBalanceLocal === null) {
            return null;
        }
        return currentBalanceLocal - this.data.wallet.sum;
    });
}
