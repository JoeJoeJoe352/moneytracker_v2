import {
    Component,
    computed,
    EventEmitter,
    inject,
    Input,
    OnChanges,
    Output,
    signal,
    Signal,
    SimpleChanges,
} from '@angular/core';
import {
    FormArray,
    FormBuilder,
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    Validators,
} from '@angular/forms';
import { NgxsmkDatepickerComponent } from 'ngxsmk-datepicker';
import { SwitchComponent } from '../../shared/components/switch.component';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { IDropdownSettings, NgMultiSelectDropDownModule } from 'ng-multiselect-dropdown';
import { DropdownInterface } from '../../shared/interfaces';
import { TransactionDetailRowComponent } from '../transaction/transaction-detail-row-component';
import { TransactionService } from '../transaction/transaction-service';
import { CategoryResponseInterface, DetailForm, NewTransaction, TransactionDataFromBackend, TransactionInputDefaultValuesWithDetails } from '../transaction/interfaces';
import { validDate } from './valid-date-validator';

@Component({
    selector: 'app-transaction-form-component',
    templateUrl: './transaction-form-component.html',
    styleUrls: ['../../shared/components/form-style.scss', './transaction-form-component.scss'],
    imports: [
    ReactiveFormsModule,
    NgxsmkDatepickerComponent,
    SwitchComponent,
    TranslatePipe,
    NgMultiSelectDropDownModule,
    TransactionDetailRowComponent,
],
})
export class TransactionFormComponent implements OnChanges {
    private fb = inject(FormBuilder);
    private transactionService = inject(TransactionService);
    private translateService = inject(TranslateService);

    /**
     * Form disabled-e (pl.: töltődéskor)
     */
    @Input({ required: true }) isTransactionFormDisabled!: boolean;
    /**
     * Kategóriák listája a selecthez
     */
    @Input({ required: true }) categoryList!: Signal<CategoryResponseInterface[]>;
    /**
     * Inputba kapott tranzakció (ha nem új tranzakcióról van szó)
     */
    @Input() transaction: TransactionDataFromBackend | null = null;
    /**
     * Kategória mentése folyamatban van-e
     */
    @Input({ required: true }) isCategorySaveInProgress!: boolean;

    /**
     * Mentés gombra kattintott a user
     */
    @Output() saved = new EventEmitter<NewTransaction>();
    /**
     * Tranzakció törlés gombra kattintott a user
     */
    @Output() deleted = new EventEmitter<number>();
    /**
     * Új kategóriát szeretne a user hozzáadni a listájához
     */
    @Output() categoryAdded = new EventEmitter<string>();

    /**
     * Tranzakciós form
     */
    protected transactionForm: FormGroup;

    /**
     * A kategória dropdown keresőmezőjébe gépelt szöveg
     * todo üres stringre állítani, ha user hozzáad elemet
     */
    protected categorySearchText = signal('');

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
        this.transactionForm = this.buildForm({
            name: '',
            isIncome: false,
            isComplexTransaction: false,
            price: null,
            transactionDate: null,
            details: [],
            categories: [],
        });
    }

    /**
     * Changes Betöltés után ha van kezdőérték beállítva, akkor a formba azokat állítjuk be
     */
    ngOnChanges(changes: SimpleChanges): void {
        if (changes['transaction']) {
            if (this.transaction === null) {
                // Nincs átadva paraméterül transaction (ngOnchanges 1x mindenképp lefut induláskor. Ez nem gond, csak NOOP)
                return;
            }
            const convertedInputValues = this.transactionService.utils.convertDataToInput(
                this.transaction,
            );

            // A meglévő form kontrollokat frissítjük a friss adatokkal, nem hozunk létre új FormGroup-ot,
            // mert az újra létrehozná a 'categories' kontrollt is, amitől az ng-multiselect-dropdown
            // "no FormControl instance attached" hibát dobna
            this.transactionForm.patchValue({
                name: convertedInputValues.name,
                isIncome: convertedInputValues.isIncome,
                isComplexTransaction: convertedInputValues.isComplexTransaction,
                price: convertedInputValues.price,
                transactionDate: convertedInputValues.transactionDate,
                categories: this.mapCategoryIdsToDropdownData(
                    convertedInputValues.categories ?? [],
                ),
            });

            this.transactionForm.setControl(
                'details',
                this.fb.array(
                    convertedInputValues.details.map((detail) => this.generateNewRow(detail)),
                ),
            );
        }
    }

    /**
     * Létrehozza a formot a validációs adatokkal
     */
    private buildForm(defaults: TransactionInputDefaultValuesWithDetails) {
        return this.fb.nonNullable.group({
            name: [
                defaults.name,
                {
                    validators: [
                        Validators.required,
                        Validators.minLength(3),
                        Validators.maxLength(200),
                    ],
                },
            ],
            isIncome: new FormControl(defaults.isIncome),
            isComplexTransaction: new FormControl(defaults.isComplexTransaction),
            price: [defaults.price, { validators: [Validators.min(1)] }],
            transactionDate: this.fb.control(defaults.transactionDate, {
                validators: [Validators.required, validDate],
            }),
            details: this.fb.array(defaults.details.map((detail) => this.generateNewRow(detail))),
            categories: this.fb.control<DropdownInterface[]>(
                this.mapCategoryIdsToDropdownData(defaults.categories ?? []),
            ),
        });
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
    onSubmit(): void {
        if (this.transactionForm.invalid) {
            this.transactionForm.markAllAsTouched();
            console.error(this.transactionForm.errors);
            return;
        }
        this.saved.emit(this.transactionForm.value);
    }

    /**
     * A kategória dropdown keresőmezőjének szövege változott
     */
    onCategoryFilterChange(filterItem: unknown): void {
        this.categorySearchText.set(filterItem as string);
    }
    /**
     * MultiselectSettings beállításai
     */
    protected multiselectSettings: Signal<IDropdownSettings> = computed(() => {
        return {
            singleSelection: false,
            idField: 'item_id',
            textField: 'item_text',
            itemsShowLimit: 3,
            allowSearchFilter: true,
            noFilteredDataAvailablePlaceholderText: this.translateService.instant(
                'transaction.category.add',
            ),
            enableCheckAll: false,
        };
    });

    /**
     * A kategória dropdown "nincs találat" sorára kattintás lekezelése (ez jelenik meg gombként, ha nincs találat)
     */
    onCategoryDropdownClick(event: Event): void {
        // kattintás esetén csak akkor ad hozzá elemet, hogyha a "nincs ilyen elem" gombra kattint
        if ((event.target as HTMLElement).closest('.no-filtered-data')) {
            const name = this.categorySearchText().trim();
            if (!name || this.isCategorySaveInProgress) {
                return;
            }
            this.categoryAdded.emit(name);
        }
    }

    /**
     * Törli a megadott indexű tétel sort
     */
    deleteRow(index: number): void {
        if (this.isLastDetailRow) {
            console.error('utolsó sort nem lehet törölni');
            return;
        }
        this.details.removeAt(index);
    }

    /**
     * Létrehoz egy új üres sort
     */
    addRow(): void {
        // todo ugorjon az oldal az aljára
        this.details.push(this.generateNewEmptyRow());
    }

    /**
     * Detail struktúra, amit új tranzakciónál, vagy új detail hozzáadásánál bővítjük vele a formot
     */
    generateNewRow(params: {
        name: string;
        price: number | null;
        weight: number | null;
        unitPrice: number | null;
        isComplexPriceMode: boolean | null;
        categories: number[] | null;
    }): FormGroup<DetailForm> {
        const detailGroup = this.fb.group({
            detailName: [params.name, Validators.required],
            detailPrice: [params.price, [Validators.min(1)]],
            detailWeight: [params.weight],
            detailUnitPrice: [params.unitPrice],
            detailIsComplexPriceMode: [params.isComplexPriceMode],
            categories: [this.mapCategoryIdsToDropdownData(params.categories ?? [])],
        }) as FormGroup<DetailForm>;

        this.setupDetailReactiveLogic(detailGroup);
        return detailGroup;
    }

    /**
     * Generál egy új input sort, üres adatokkal
     */
    generateNewEmptyRow() {
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
     * Létező tranzakció adatai vannak-e a formban (+ guard)
     */
    isExistingTransaction(): this is { transaction: TransactionDataFromBackend } {
        return this.transaction !== null;
    }

    /**
     * A detailhez tartozó logikát beállítja (ha a price inputba gépelünk, akkor a weight és unit price inputok letiltódnak, és fordítva)
     */
    private setupDetailReactiveLogic(detailGroup: FormGroup<DetailForm>) {
        const priceControl = detailGroup.controls.detailPrice;
        const unitControl = detailGroup.controls.detailUnitPrice;
        const weightControl = detailGroup.controls.detailWeight;
        const isComplexModeControl = detailGroup.controls.detailIsComplexPriceMode;

        // már létező tranzakciónál a disabled/enabled-ek beállítása
        if (isComplexModeControl.value) {
            priceControl.disable({ emitEvent: false });
        } else {
            // Az ár ki van írva a usernek mindenképpen, ezért valid állapot, hogy mindhárom adat ki van töltve, ezért itt elég csak a price-t disabled-re tenni
            unitControl.disable({ emitEvent: false });
            weightControl.disable({ emitEvent: false });
        }

        // PRICE inputba gépelés
        isComplexModeControl.valueChanges.subscribe((isComplexMode) => {
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
        });
    }

    // Getters

    /**
     * Utolsó detail sor nem törölhető, ezért a gombot letiltjuk, ha csak 1 sor van
     */
    get isLastDetailRow() {
        return this.details.length < 2;
    }

    get name(): FormControl<string> {
        return this.transactionForm.get('name') as FormControl<string>;
    }

    get price(): FormControl<number | null> {
        return this.transactionForm.get('price') as FormControl<number | null>;
    }

    get categories(): FormControl<DropdownInterface[]> {
        return this.transactionForm.get('categories') as FormControl<DropdownInterface[]>;
    }

    get transactionDate(): FormControl<Date | null> {
        return this.transactionForm.get('transactionDate') as FormControl<Date | null>;
    }

    get isIncome(): FormControl<boolean> {
        return this.transactionForm.get('isIncome') as FormControl<boolean>;
    }

    get isComplexTransaction(): FormControl<boolean> {
        return this.transactionForm.get('isComplexTransaction') as FormControl<boolean>;
    }

    get details(): FormArray<FormGroup<DetailForm>> {
        return this.transactionForm.get('details') as FormArray<FormGroup<DetailForm>>;
    }
}
