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
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { greaterThan } from './greater-than-validator';
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
import { validDate } from './valid-date-validator';
import { UserDataStore } from '@shared/services/user-data-store';
import { WalletDataUtil } from '../../wallet/wallet-data-util';
import { MatDialogModule } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { TransactionUtils } from '../transaction-utils';
import { toSignal } from '@angular/core/rxjs-interop';

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
     * Az "új tétel" gomb sora, hogy addRow()-nál az oldal aljára tudjunk görgetni
     */
    private readonly detailAddButton = viewChild<ElementRef<HTMLElement>>('detailAddButton');

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

    /**
     * Changes Betöltés után ha van kezdőérték beállítva, akkor a formba azokat állítjuk be
     */
    ngOnChanges(changes: SimpleChanges): void {
        if (changes['isTransactionFormDisabled']) {
            if (this.isTransactionFormDisabled()) {
                this.transactionForm.disable();
            } else {
                this.transactionForm.enable();
                // enable() minden leszármazott kontrollt enged, ezért az ár megadási mód szerinti
                // disabled állapotot vissza kell állítani soronként
                this.details.controls.forEach((detailGroup) =>
                    this.applyDetailRowPriceModeDisabledState(
                        detailGroup,
                        detailGroup.controls.detailIsComplexPriceMode.value,
                    ),
                );
            }
        }
        if (changes['transaction']) {
            const transaction = this.transaction();
            if (transaction === null) {
                // Nincs átadva paraméterül transaction (ngOnchanges 1x mindenképp lefut induláskor. Ez nem gond, csak NOOP)
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
        // A form típusa szerint minden mező opcionális (a letiltott kontrollok kimaradnak a .value-ból),
        // de érvényes formnál a validátorok miatt a kötelező mezők ki vannak töltve
        this.saved.emit(this.transactionForm.value as NewTransaction);
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
            name: [
                '',
                {
                    validators: [
                        Validators.required,
                        Validators.minLength(3),
                        Validators.maxLength(120),
                    ],
                },
            ],
            isIncome: [false],
            isComplexTransaction: [false],
            price: this.fb.control<number | null>(null, {
                validators: [Validators.required, greaterThan(0)],
            }),
            transactionDate: this.fb.control(new Date(), {
                validators: [Validators.required, validDate],
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
        const detailGroup = this.fb.nonNullable.group({
            detailName: [
                params.name,
                {
                    validators: [
                        Validators.required,
                        Validators.minLength(3),
                        Validators.maxLength(20),
                    ],
                },
            ],
            detailPrice: this.fb.control<number | null>(params.price, [
                Validators.required,
                greaterThan(0),
            ]),
            detailWeight: this.fb.control<number | null>(params.weight, [
                Validators.required,
                greaterThan(0),
            ]),
            detailUnitPrice: this.fb.control<number | null>(params.unitPrice, [
                Validators.required,
                greaterThan(0),
            ]),
            detailIsComplexPriceMode: [params.isComplexPriceMode ?? false],
            categories: this.fb.nonNullable.control(
                this.mapCategoryIdsToDropdownData(params.categories ?? []),
            ),
        });

        this.setupDetailReactiveLogic(detailGroup);
        return detailGroup;
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
     * A detailhez tartozó logikát beállítja (ha a price inputba gépelünk, akkor a weight és unit price inputok letiltódnak, és fordítva)
     */
    private setupDetailReactiveLogic(detailGroup: FormGroup<DetailForm>) {
        const isComplexModeControl = detailGroup.controls.detailIsComplexPriceMode;

        // már létező tranzakciónál a disabled/enabled-ek beállítása
        this.applyDetailRowPriceModeDisabledState(detailGroup, isComplexModeControl.value);

        // PRICE inputba gépelés
        isComplexModeControl.valueChanges.subscribe((isComplexMode) => {
            this.applyDetailRowPriceModeDisabledState(detailGroup, isComplexMode);
        });
    }

    /**
     * A price/weight/unitPrice kontrollok disabled állapotát állítja be az ár megadási mód alapján.
     * Kell akkor is, ha a teljes form disable()/enable()-je felülírná ezt az állapotot.
     */
    private applyDetailRowPriceModeDisabledState(
        detailGroup: FormGroup<DetailForm>,
        isComplexMode: boolean | null,
    ): void {
        const priceControl = detailGroup.controls.detailPrice;
        const unitControl = detailGroup.controls.detailUnitPrice;
        const weightControl = detailGroup.controls.detailWeight;

        if (isComplexMode) {
            // emitEvent azért kell, hogy disable ne emiteljen egy újabb change-t, mert akkor végtelen ciklusba kerülünk
            priceControl.disable({ emitEvent: false });
            weightControl.enable({ emitEvent: false });
            unitControl.enable({ emitEvent: false });
        } else {
            priceControl.enable({ emitEvent: false });
            unitControl.disable({ emitEvent: false });
            weightControl.disable({ emitEvent: false });
        }
    }

    /**
     * Komplex tranzakció kapcsoló állításakor létrehoz egy új üres sort, hogyha nincs (default nincs, vagy ha törölte a user a sort)
     */
    protected handleIsComplexTransactionToggle() {
        if (this.isComplexTransaction.value && this.details.length === 0) {
            this.addRow();
        }
    }

    // Getters

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
