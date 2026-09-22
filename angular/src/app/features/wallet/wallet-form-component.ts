import {
    Component,
    inject,
    OnInit,
    output,
    Signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { WalletCreateRequest, WalletDataInterface, WalletUpdateRequest } from './interfaces';
import { CurrencyCodesEnum, WalletTypesEnum } from '@shared/enums';
import { WalletDataUtil } from './wallet-data-util';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { DialogCloseButton } from '@shared/components/mat-modal-close';

export interface WalletFormInputInterface {
    wallet: WalletDataInterface | null;
    isFormDisabled: Signal<boolean>;
}

const WALLET_TYPE_TO_DESCRIPTION: Record<WalletTypesEnum, string> = {
    [WalletTypesEnum.default]: 'wallet.type.description.default',
    [WalletTypesEnum.savings]: 'wallet.type.description.savings',
};

@Component({
    selector: 'app-wallet-form-component',
    templateUrl: './wallet-form-component.html',
    styleUrls: ['./wallet-form-component.scss'],
    standalone: true,
    imports: [
        ReactiveFormsModule,
        TranslatePipe,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatButtonModule,
        MatDialogModule,
        MatIconModule,
        DialogCloseButton,
    ],
})
export class WalletFormComponent implements OnInit {
    private readonly fb = inject(FormBuilder);
    protected readonly walletDataUtil = inject(WalletDataUtil);
    protected readonly currencyOptions = Object.values(CurrencyCodesEnum);
    protected readonly walletTypeOptions = Object.values(WalletTypesEnum);

    public data = inject<WalletFormInputInterface>(MAT_DIALOG_DATA);

    /**
     * Ha meg van adva, akkor a form szerkesztő módban nyílik, egyébként létrehozó módban
     */
    protected wallet: WalletDataInterface | null = null;

    public deleted = output<number>();
    public saved = output<WalletCreateRequest | WalletUpdateRequest>();

    protected walletForm = this.fb.nonNullable.group({
        name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(20)]],
        currencyCode: [CurrencyCodesEnum.huf, [Validators.required]],
        walletType: [WalletTypesEnum.default, [Validators.required]],
    });

    ngOnInit(): void {
        this.wallet = this.data.wallet;
        if (this.wallet) {
            this.walletForm.patchValue({
                name: this.wallet.name,
                currencyCode: this.wallet.currencyCode,
                walletType: this.wallet.type,
            });
            // Mert egyenlőre nem akarom lekezelni mi lenne a tárcában szereplő tranzakciókkal, ha valutát váltana
            this.walletForm.controls.currencyCode.disable();
        }
    }

    /**
     * Meglévő walletet szerkesztünk-e
     */
    protected isEditMode(): this is { wallet: WalletDataInterface } {
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
            this.isEditMode()
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
    protected getDescriptionTranslateKeyForWalletType = () =>
        WALLET_TYPE_TO_DESCRIPTION[this.walletType.value] ?? '';

    get name() {
        return this.walletForm.controls.name;
    }

    get walletType() {
        return this.walletForm.controls.walletType;
    }
}
