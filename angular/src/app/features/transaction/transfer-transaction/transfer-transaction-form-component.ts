import {
    Component,
    computed,
    inject,
    input,
    OnChanges,
    output,
    SimpleChanges,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { TranslatePipe } from '@ngx-translate/core';
import { UserDataStore } from '@app/shared/stores/user-data-store';
import { WalletDataUtil } from '@shared/utils/wallet-data-util';
import {
    POSITIVE_AMOUNT_VALIDATORS,
    TRANSACTION_DATE_VALIDATORS,
    TRANSACTION_NAME_VALIDATORS,
} from '../transaction-form/transaction-field-validators';
import { transferWalletsValidator } from './transfer-wallets-validator';
import { TransferTransactionFormValue, TransferTransactionResponse } from './interfaces';
import { TimeHelper } from '@shared/services/time-helper';

/**
 * Átvezetés form: a forrás walletből a cél walletbe kerül az összeg. Mindkét
 * oldalnak saját neve van, a dátum és az összeg közös
 */
@Component({
    selector: 'app-transfer-transaction-form-component',
    templateUrl: './transfer-transaction-form-component.html',
    styleUrl: './transfer-transaction-form-component.scss',
    imports: [
        ReactiveFormsModule,
        MatDatepickerModule,
        MatInputModule,
        MatFormFieldModule,
        MatSelectModule,
        MatButtonModule,
        MatDialogModule,
        TranslatePipe,
    ],
})
export class TransferTransactionFormComponent implements OnChanges {
    private readonly fb = inject(FormBuilder);
    protected readonly userData = inject(UserDataStore);
    private readonly walletUtil = inject(WalletDataUtil);
    private readonly timeHelper = inject(TimeHelper);

    /**
     * Datepickerben utolsó kiválasztható nap
     */
    protected readonly lastSelectableDay = new Date();
    /**
     * Form disabled-e (pl.: mentés közben)
     */
    public readonly isFormDisabled = input.required<boolean>();
    /**
     * Szerkesztendő átvezetés, null esetén új átvezetés létrehozása
     */
    public readonly transfer = input<TransferTransactionResponse | null>(null);
    /**
     * Mentés gombra kattintott a user, érvényes formmal
     */
    public readonly saved = output<TransferTransactionFormValue>();
    /**
     * Törlés gombra kattintott a user (a megerősítést a hívó kéri)
     */
    public readonly deleted = output<void>();

    protected readonly form = this.fb.group(
        {
            srcTransactionName: this.fb.nonNullable.control('', TRANSACTION_NAME_VALIDATORS),
            srcWalletId: this.fb.control<number | null>(this.userData.getDefaultWallet().id),
            dstTransactionName: this.fb.nonNullable.control('', TRANSACTION_NAME_VALIDATORS),
            dstWalletId: this.fb.control<number | null>(null, Validators.required),
            transactionDate: this.fb.control<Date | null>(new Date(), TRANSACTION_DATE_VALIDATORS),
            priceSum: this.fb.control<number | null>(null, POSITIVE_AMOUNT_VALIDATORS),
        },
        { validators: transferWalletsValidator(() => this.userData.getWallets()) },
    );

    /**
     * Kiindulási wallet dropdown opciói
     */
    protected srcWalletOptions = computed(() => {
        return this.userData.getWallets();
    });

    /**
     * Érkezési wallet dropdown opciói (Filterezve az src és az eltérő valutájú walletek)
     */
    protected dstWalletOptions = computed(() =>
        this.getDstWalletOptions(this.selectedSrcWalletId()),
    );

    /**
     * A kiválasztott forrás wallet azonosítója
     */
    private readonly selectedSrcWalletId = toSignal(this.form.controls.srcWalletId.valueChanges, {
        initialValue: this.form.controls.srcWalletId.value,
    });

    /**
     * A kiválasztott forrás wallet adatai
     */
    protected readonly selectedSrcWalletData = computed(() =>
        this.findWallet(this.selectedSrcWalletId()),
    );

    /**
     * A forrás wallet pénznemének szimbóluma (a cél walletnek ugyanez kell legyen)
     */
    protected readonly currencySymbol = computed(() => {
        const selectedSrcWalletData = this.selectedSrcWalletData();
        return selectedSrcWalletData
            ? this.walletUtil.getCurrencySymbolForCurrencyCode(selectedSrcWalletData.currencyCode)
            : '';
    });

    constructor() {
        // Forrás wallet váltásakor a cél walletet töröljük, ha az már nem választható
        // (ugyanaz a wallet, vagy eltérő a pénznem), különben a form csendben invalid maradna
        this.srcWalletId.valueChanges.pipe(takeUntilDestroyed()).subscribe((srcWalletId) => {
            const dstWalletId = this.dstWalletId.value;
            const isDstStillValid = this.getDstWalletOptions(srcWalletId).some(
                (wallet) => wallet.id === dstWalletId,
            );
            if (dstWalletId !== null && !isDstStillValid) {
                this.dstWalletId.setValue(null);
            }
        });
    }

    /**
     * A megadott forrás wallethez választható cél walletek (az src és az eltérő pénznemű
     * walletek nélkül)
     */
    private getDstWalletOptions(srcWalletId: number | null) {
        const srcWallet = this.findWallet(srcWalletId);

        if (!srcWallet) {
            throw new Error('There is no wallet for the user with this id: ' + srcWalletId);
        }

        return this.userData
            .getWallets()
            .filter(
                (wallet) =>
                    wallet.id !== srcWallet.id && wallet.currencyCode === srcWallet.currencyCode,
            );
    }

    private findWallet(walletId: number | null) {
        return this.userData.getWallets().find((wallet) => wallet.id === walletId);
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['isFormDisabled']) {
            if (this.isFormDisabled()) {
                this.form.disable();
            } else {
                this.form.enable();
            }
        }

        const transfer = this.transfer();
        const isEditMode = transfer !== null;
        if (isEditMode && changes['transfer']) {
            this.form.patchValue({
                srcTransactionName: transfer.srcTransactionName,
                srcWalletId: transfer.srcWalletId,
                dstTransactionName: transfer.dstTransactionName,
                dstWalletId: transfer.dstWalletId,
                transactionDate: this.timeHelper.stringToDate(transfer.transactionDate),
                priceSum: transfer.priceSum,
            });
        }

        // Meglévő átvezetés walletjei nem módosíthatók (a backend sem engedi). Ngonchanges végén
        // kell letiltani, mert a fenti enable() újra engedélyezné
        if (isEditMode) {
            this.srcWalletId.disable({ emitEvent: false });
            this.dstWalletId.disable({ emitEvent: false });
        }
    }

    /**
     * Form elküldésekori műveletek
     */
    protected onSubmit(): void {
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }
        // A cast azért kell, mert az érvényes form kötelező mezői már nem lehetnek null-ok
        this.saved.emit(this.form.getRawValue() as TransferTransactionFormValue);
    }

    // Getterek

    get srcTransactionName() {
        return this.form.controls.srcTransactionName;
    }

    get srcWalletId() {
        return this.form.controls.srcWalletId;
    }

    get dstTransactionName() {
        return this.form.controls.dstTransactionName;
    }

    get dstWalletId() {
        return this.form.controls.dstWalletId;
    }

    get transactionDate() {
        return this.form.controls.transactionDate;
    }

    get priceSum() {
        return this.form.controls.priceSum;
    }
}
