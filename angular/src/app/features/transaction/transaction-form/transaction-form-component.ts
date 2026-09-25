import {
    Component,
    computed,
    ElementRef,
    inject,
    input,
    OnChanges,
    output,
    Signal,
    SimpleChanges,
    viewChild,
} from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
    DETAIL_NAME_VALIDATORS,
    DETAIL_PRICE_VALIDATORS,
    DETAIL_WEIGHT_AND_UNIT_PRICE_VALIDATORS,
    GLOBAL_PRICE_VALIDATORS,
    TRANSACTION_DATE_VALIDATORS,
    TRANSACTION_NAME_VALIDATORS,
} from './transaction-field-validators';
import { updateTreeValidity } from '@shared/utils/form-util';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';
import { DropdownInterface } from '@shared/interfaces';
import { TransactionDetailFormComponent } from './transaction-detail-form-component';
import { CategorySelectComponent } from './category-select-component';
import {
    CategoryResponseInterface,
    DetailForm,
    NewTransaction,
    TransactionDataFromBackend,
    TransactionInputDefaultValuesWithDetails,
} from '../interfaces';
import { UserDataStore } from '@app/shared/stores/user-data-store';
import { WalletDataUtil } from '../../wallet/wallet-data-util';
import { MatDialogModule } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { TransactionUtils } from '../transaction-utils';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';

@Component({
    selector: 'app-transaction-form-component',
    templateUrl: './transaction-form-component.html',
    styleUrls: ['./transaction-form-component.scss'],
    imports: [
        ReactiveFormsModule,
        MatDatepickerModule,
        MatInputModule,
        MatFormFieldModule,
        MatSelectModule,
        MatButtonModule,
        MatIconModule,
        MatSlideToggleModule,
        TranslatePipe,
        TransactionDetailFormComponent,
        CategorySelectComponent,
        MatDialogModule,
    ],
})
export class TransactionFormComponent implements OnChanges {
    private readonly fb = inject(FormBuilder);
    protected readonly userData = inject(UserDataStore);
    protected readonly walletUtil = inject(WalletDataUtil);
    protected readonly transactionUtils = inject(TransactionUtils);

    /**
     * Az "új tétel" gomb sora, hogy addRow()-nál az oldal aljára tudjunk görgetni
     */
    private readonly detailAddButton = viewChild<ElementRef<HTMLElement>>('detailAddButton');

    /**
     * Form disabled-e (pl.: töltődéskor)
     */
    public isTransactionFormDisabled = input.required<boolean>();
    /**
     * Kategóriák listája a selecthez
     */
    public categoryList = input.required<CategoryResponseInterface[]>();
    /**
     * Inputba kapott tranzakció (ha nem új tranzakcióról van szó)
     */
    public transaction = input<TransactionDataFromBackend | null>(null);
    /**
     * Kategória mentése folyamatban van-e
     */
    public isCategorySaveInProgress = input.required<boolean>();

    public addCategoryCallback =
        input.required<(name: string) => Observable<CategoryResponseInterface>>();
    /**
     * Mentés gombra kattintott a user
     */
    public saved = output<NewTransaction>();
    /**
     * Tranzakció törlés gombra kattintott a user
     */
    public deleted = output<number>();

    /**
     * Tranzakciós form. A selectedWalletId előtt kell létrehozni, mert annak a valueChanges-ét figyeli
     */
    protected readonly transactionForm = this.createForm();

    /**
     * A formban kiválasztott wallet azonosítója (a form változásaira, pl. select váltásra vagy
     * meglévő tranzakció betöltésére magától frissül)
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
     * Kategória adatokat átalakítja a dropdown számára értelmezhető formátumra
     */
    protected categoryData: Signal<DropdownInterface[]> = computed(() => {
        return this.categoryList().map((category) => {
            return {
                item_id: category.id,
                item_text: category.name,
            };
        });
    });

    constructor() {
        // A mezők validátorai a tranzakció és a detail sorok ár megadási módjától függenek
        // (lásd transaction-field-validators), ezért bármilyen változás után újravalidálunk.
        // Ide fut be a kapcsolók állítása, sor hozzáadás/törlés és a tranzakció betöltése is
        updateTreeValidity(this.transactionForm);
        this.transactionForm.valueChanges
            .pipe(takeUntilDestroyed())
            .subscribe(() => updateTreeValidity(this.transactionForm));
    }

    ngOnChanges(changes: SimpleChanges): void {
        // Betöltés után ha van kezdőérték beállítva, akkor a formba azokat állítjuk be
        if (changes['isTransactionFormDisabled']) {
            if (this.isTransactionFormDisabled()) {
                this.transactionForm.disable();
            } else {
                this.transactionForm.enable();
            }
        }

        if (changes['transaction']) {
            const transaction = this.transaction();
            if (transaction === null) {
                // Nincs átadva paraméterül transaction, mert új tranzakciót hozunk létre
                return;
            }
            const convertedInputValues = this.transactionUtils.convertDataToInput(transaction);
            this.refreshFormWithData(convertedInputValues);
        }
    }

    /**
     * Meglévő form elemeket frissíti a megadot adatokkal
     */
    private refreshFormWithData(inputValues: TransactionInputDefaultValuesWithDetails) {
        this.transactionForm.patchValue({
            name: inputValues.name,
            isIncome: inputValues.isIncome,
            isComplexTransaction: inputValues.isComplexTransaction,
            price: inputValues.price,
            transactionDate: inputValues.transactionDate,
            categories: this.mapCategoryIdsToDropdownData(inputValues.categories ?? []),
            walletId: inputValues.walletId,
        });

        this.transactionForm.setControl(
            'details',
            this.fb.array(inputValues.details.map((detail) => this.generateNewRow(detail))),
        );
    }

    /**
     * Kategória id-kat alakítja át a dropdown által elvárt {item_id, item_text} formátumra.
     */
    private mapCategoryIdsToDropdownData(ids: number[]): DropdownInterface[] {
        return ids.length > 0
            ? this.categoryData().filter((category) => ids.includes(category.item_id))
            : [];
    }

    /**
     * Form elküldésekori műveletek
     */
    protected onSubmit(): void {
        if (this.transactionForm.invalid) {
            this.transactionForm.markAllAsTouched();
            return;
        }
        // Minden mező értékét elküldjük, a transactionUtils a módok alapján választja ki a mérvadókat.
        // A cast azért kell, mert az érvényes form kötelező mezői már nem lehetnek null-ok
        this.saved.emit(this.transactionForm.getRawValue() as NewTransaction);
    }

    /**
     * Törli a megadott indexű tétel sort
     */
    protected deleteRow(index: number): void {
        if (this.isLastDetailRow) {
            console.error('utolsó sort nem lehet törölni');
            return;
        }
        this.details.removeAt(index);
    }

    /**
     * Létrehoz egy új üres sort
     */
    protected addRow(): void {
        this.details.push(this.generateNewEmptyRow());
        setTimeout(() =>
            this.detailAddButton()?.nativeElement.scrollIntoView({
                behavior: 'smooth',
                block: 'nearest',
            }),
        );
    }

    /**
     * Létrehozza a formot a validációs adatokkal
     */
    private createForm() {
        return this.fb.nonNullable.group({
            name: ['', { validators: TRANSACTION_NAME_VALIDATORS }],
            isIncome: [false],
            isComplexTransaction: [false],
            price: this.fb.control<number | null>(null, {
                validators: GLOBAL_PRICE_VALIDATORS,
            }),
            transactionDate: this.fb.control(new Date(), {
                validators: TRANSACTION_DATE_VALIDATORS,
            }),
            walletId: [this.userData.getDefaultWallet().id],
            details: new FormArray<FormGroup<DetailForm>>([]),
            categories: this.fb.nonNullable.control<DropdownInterface[]>([]),
        });
    }

    /**
     * Detail struktúra, amit új tranzakciónál, vagy új detail hozzáadásánál bővítjük vele a formot
     */
    private generateNewRow(params: {
        name: string;
        price: number | null;
        weight: number | null;
        unitPrice: number | null;
        isComplexPriceMode: boolean | null;
        categories: number[] | null;
    }): FormGroup<DetailForm> {
        return this.fb.nonNullable.group({
            detailName: [params.name, { validators: DETAIL_NAME_VALIDATORS }],
            detailPrice: this.fb.control<number | null>(params.price, DETAIL_PRICE_VALIDATORS),
            detailWeight: this.fb.control<number | null>(
                params.weight,
                DETAIL_WEIGHT_AND_UNIT_PRICE_VALIDATORS,
            ),
            detailUnitPrice: this.fb.control<number | null>(
                params.unitPrice,
                DETAIL_WEIGHT_AND_UNIT_PRICE_VALIDATORS,
            ),
            detailIsComplexPriceMode: [params.isComplexPriceMode ?? false],
            categories: this.fb.nonNullable.control(
                this.mapCategoryIdsToDropdownData(params.categories ?? []),
            ),
        });
    }

    /**
     * Generál egy új input sort, üres adatokkal
     */
    private generateNewEmptyRow() {
        return this.generateNewRow({
            name: '',
            price: null,
            unitPrice: null,
            weight: null,
            isComplexPriceMode: false,
            categories: [],
        });
    }

    /**
     * Komplex tranzakció kapcsoló állításakor létrehoz egy új üres sort, hogyha nincs (default nincs, vagy ha törölte a user a sort)
     */
    protected handleIsComplexTransactionToggle() {
        if (this.isComplexTransaction.value && this.details.length === 0) {
            this.addRow();
        }
    }

    // Getterek

    /**
     * Utolsó detail sor nem törölhető, ezért a gombot letiltjuk, ha csak 1 sor van
     */
    get isLastDetailRow() {
        return this.details.length <= 1;
    }

    get walletId() {
        return this.transactionForm.controls.walletId;
    }

    get name() {
        return this.transactionForm.controls.name;
    }

    get price() {
        return this.transactionForm.controls.price;
    }

    get categories() {
        return this.transactionForm.controls.categories;
    }

    get transactionDate() {
        return this.transactionForm.controls.transactionDate;
    }

    get isIncome() {
        return this.transactionForm.controls.isIncome;
    }

    get isComplexTransaction() {
        return this.transactionForm.controls.isComplexTransaction;
    }

    get details() {
        return this.transactionForm.controls.details;
    }
}
