import {
    Component,
    computed,
    inject,
    input,
    OnChanges,
    output,
    Signal,
    SimpleChanges,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialogModule } from '@angular/material/dialog';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import {
    POSITIVE_AMOUNT_VALIDATORS,
    TRANSACTION_DATE_VALIDATORS,
    TRANSACTION_NAME_VALIDATORS,
} from '../transaction/transaction-form/transaction-field-validators';
import { CategorySelectComponent } from '../transaction/transaction-form/category-select-component';
import { CategoryResponseInterface } from '../transaction/interfaces';
import {
    RecurringTransactionResponseInterface,
    RecurringTransactionSaveRequestInterface,
} from './interfaces';
import { DropdownInterface } from '@shared/interfaces';
import {
    RecurrenceIntervalEnum,
    RecurringTransactionStatusEnum,
    TransactionTypeEnum,
} from '@shared/enums';
import { UserDataStore } from '@shared/stores/user-data-store';
import { WalletDataUtil } from '@shared/utils/wallet-data-util';
import { TimeHelper } from '@shared/services/time-helper';

@Component({
    selector: 'app-recurring-transaction-form-component',
    templateUrl: './recurring-transaction-form-component.html',
    styleUrl: './recurring-transaction-form-component.scss',
    imports: [
        ReactiveFormsModule,
        MatDatepickerModule,
        MatInputModule,
        MatFormFieldModule,
        MatSelectModule,
        MatButtonModule,
        MatSlideToggleModule,
        MatDialogModule,
        TranslatePipe,
        CategorySelectComponent,
    ],
})
export class RecurringTransactionFormComponent implements OnChanges {
    private readonly fb = inject(FormBuilder);
    private readonly timeHelper = inject(TimeHelper);
    protected readonly userData = inject(UserDataStore);
    protected readonly walletUtil = inject(WalletDataUtil);

    protected readonly recurrenceIntervals = Object.values(RecurrenceIntervalEnum);

    /**
     * Form disabled-e (pl.: mentéskor)
     */
    public isFormDisabled = input.required<boolean>();
    /**
     * Kategóriák listája a selecthez
     */
    public categoryList = input.required<CategoryResponseInterface[]>();
    /**
     * Szerkesztendő ismétlődő tranzakció. Ha null, akkor új létrehozása történik
     */
    public recurringTransaction = input<RecurringTransactionResponseInterface | null>(null);
    /**
     * Kategória mentése folyamatban van-e
     */
    public isCategorySaveInProgress = input.required<boolean>();

    public addCategoryCallback =
        input.required<(name: string) => Observable<CategoryResponseInterface>>();
    /**
     * Mentés gombra kattintott a user, a backend által várt formátumban adja át az adatokat
     */
    public saved = output<RecurringTransactionSaveRequestInterface>();

    /**
     * Form. A selectedWalletId előtt kell létrehozni, mert annak a valueChanges-ét figyeli
     */
    protected readonly form = this.createForm();

    /**
     * A formban kiválasztott wallet azonosítója
     */
    private readonly selectedWalletId = toSignal(this.walletId.valueChanges, {
        initialValue: this.walletId.value,
    });

    /**
     * A kiválasztott wallet pénznemének szimbóluma
     */
    protected readonly selectedWalletsCurrency = computed(() => {
        const selectedWallet = this.userData
            .getWallets()
            .find((wallet) => wallet.id === this.selectedWalletId());

        return selectedWallet
            ? this.walletUtil.getCurrencySymbolForCurrencyCode(selectedWallet.currencyCode)
            : '';
    });

    /**
     * Leállított állapotú-e a szerkesztett ismétlődő tranzakció (csak ekkor indítható újra)
     */
    protected readonly isStopped = computed(
        () => this.recurringTransaction()?.status === RecurringTransactionStatusEnum.STOPPED,
    );

    /**
     * Kategória adatokat átalakítja a dropdown számára értelmezhető formátumra
     */
    protected categoryData: Signal<DropdownInterface[]> = computed(() => {
        return this.categoryList().map((category) => ({
            item_id: category.id,
            item_text: category.name,
        }));
    });

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['isFormDisabled']) {
            if (this.isFormDisabled()) {
                this.form.disable();
            } else {
                this.form.enable();
            }
        }

        if (changes['recurringTransaction']) {
            const recurringTransaction = this.recurringTransaction();
            if (recurringTransaction !== null) {
                this.refreshFormWithData(recurringTransaction);
            }
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
        const value = this.form.getRawValue();
        // Érvényes formban a kötelező mezők már nem lehetnek null-ok
        const price = value.price!;
        const date = value.date!;

        this.saved.emit({
            name: value.name,
            // Backend előjelesen tárolja az összeget, a kiadás negatív
            priceSum: value.isIncome ? price : price * -1,
            startDate: this.timeHelper.formatDateToString(date),
            recurrenceInterval: value.recurrenceInterval,
            transactionType: value.isIncome
                ? TransactionTypeEnum.INCOME
                : TransactionTypeEnum.OUTCOME,
            categoryIds: value.categories.map((category) => category.item_id),
            walletId: value.walletId,
            isRestart: this.isStopped() && value.isRestart,
        });
    }

    /**
     * Form feltöltése a szerkesztett ismétlődő tranzakció adataival
     */
    private refreshFormWithData(recurringTransaction: RecurringTransactionResponseInterface) {
        const isIncome = recurringTransaction.transactionType === TransactionTypeEnum.INCOME;
        const categoryIds = recurringTransaction.categories.map((category) => category.id);

        this.form.patchValue({
            name: recurringTransaction.name,
            isIncome: isIncome,
            // Frontenden mindig pozitív összeg szerepel az inputban
            price: isIncome ? recurringTransaction.priceSum : recurringTransaction.priceSum * -1,
            // A backend a következő esedékességhez hasonlít: ha ez változik, újraállítja a dátumokat
            date: this.timeHelper.stringToDate(recurringTransaction.nextOccurrenceDate),
            recurrenceInterval: recurringTransaction.recurrenceInterval,
            walletId: recurringTransaction.wallet.id,
            categories: this.categoryData().filter((category) =>
                categoryIds.includes(category.item_id),
            ),
            isRestart: false,
        });
    }

    /**
     * Létrehozza a formot a validációs adatokkal
     */
    private createForm() {
        return this.fb.nonNullable.group({
            name: ['', { validators: TRANSACTION_NAME_VALIDATORS }],
            isIncome: [false],
            price: this.fb.control<number | null>(null, { validators: POSITIVE_AMOUNT_VALIDATORS }),
            date: this.fb.control<Date | null>(new Date(), {
                validators: TRANSACTION_DATE_VALIDATORS,
            }),
            recurrenceInterval: [RecurrenceIntervalEnum.MONTHLY],
            walletId: [this.userData.getDefaultWallet().id],
            categories: this.fb.nonNullable.control<DropdownInterface[]>([]),
            isRestart: [false],
        });
    }

    // Getterek

    get name() {
        return this.form.controls.name;
    }

    get price() {
        return this.form.controls.price;
    }

    get date() {
        return this.form.controls.date;
    }

    get isIncome() {
        return this.form.controls.isIncome;
    }

    get walletId() {
        return this.form.controls.walletId;
    }
}
