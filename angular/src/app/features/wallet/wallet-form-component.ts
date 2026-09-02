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
            // Egyenlőre nem akarom lekezelni mi lenne a tárcában szereplő tranzakciókkal, ha valutát váltana
            this.walletForm.controls.currencyCode.disable();
        }
    }

    /**
     * Meglévő walletet szerkesztünk-e
     */
    protected get isEditMode(): boolean {
        return this.wallet !== null;
    }

    /**
     * Form adatainak elküldése
     */
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

    /**
     * Visszaadja a nyelvi kulcsot, amely a wallet típusának leírását tartalmazza
     */
    protected getDescriptionTranslateKeyForWalletType(): string {
        switch (this.walletType.value) {
            case WalletTypesEnum.default:
                return 'wallet.type.description.default';
            case WalletTypesEnum.savings:
                return 'wallet.type.description.savings';
            default:
                throw Error('No description for the type: ' + this.walletType.value);
        }
    }

    get name() {
        return this.walletForm.controls.name;
    }

    get walletType() {
        return this.walletForm.controls.walletType;
    }
}
