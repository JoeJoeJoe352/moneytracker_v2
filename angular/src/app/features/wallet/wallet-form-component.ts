import { Component, EventEmitter, inject, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { WalletCreateRequest, WalletDataInterface, WalletUpdateRequest } from './interfaces';
import { CurrencyCodesEnum, WalletTypesEnum } from '../../shared/enums';

@Component({
    selector: 'app-wallet-form-component',
    templateUrl: './wallet-form-component.html',
    styleUrls: ['../../shared/components/form-style.scss'],
    standalone: true,
    imports: [ReactiveFormsModule, TranslatePipe],
})
export class WalletFormComponent implements OnInit {
    private fb = inject(FormBuilder);

    /**
     * Ha meg van adva, akkor a form szerkesztő módban nyílik, egyébként létrehozó módban
     */
    @Input() wallet: WalletDataInterface | null = null;
    @Input({ required: true }) isFormDisabled!: boolean;

    @Output() saved = new EventEmitter<WalletCreateRequest | WalletUpdateRequest>();
    @Output() deleted = new EventEmitter<number>();

    protected readonly currencyOptions = Object.values(CurrencyCodesEnum);
    protected readonly walletTypeOptions = Object.values(WalletTypesEnum);

    protected walletForm = this.fb.nonNullable.group({
        name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
        currencyCode: [CurrencyCodesEnum.huf, [Validators.required]],
        walletType: [WalletTypesEnum.default, [Validators.required]],
    });

    ngOnInit(): void {
        if (this.wallet) {
            this.walletForm.patchValue({
                name: this.wallet.name,
                currencyCode: this.wallet.currencyCode,
                walletType: this.wallet.type,
            });
            // Pénznemet létrehozás után nem lehet módosítani
            this.walletForm.controls.currencyCode.disable();
        }
    }

    protected get isEditMode(): boolean {
        return this.wallet !== null;
    }

    onSubmit(): void {
        if (this.walletForm.invalid) {
            this.walletForm.markAllAsTouched();
            return;
        }

        const value = this.walletForm.getRawValue();

        this.saved.emit(
            this.isEditMode
                ? { name: value.name, walletType: value.walletType }
                : {
                      name: value.name,
                      currencyCode: value.currencyCode,
                      walletType: value.walletType,
                  },
        );
    }

    get name() {
        return this.walletForm.controls.name;
    }
}
