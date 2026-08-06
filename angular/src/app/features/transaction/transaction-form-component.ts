import {
    Component,
    EventEmitter,
    inject,
    Input,
    OnChanges,
    Output,
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
import { validDate } from './valid-date-validator';
import { TransactionService } from './transaction-service';
import {
    NewTransaction,
    TransactionDataFromBackend,
    TransactionInputDefaultValuesWithDetails,
} from './interfaces';
import { TranslatePipe } from '@ngx-translate/core';

/**
 * Detail form elemei
 */
interface DetailForm {
    detailName: FormControl<string>;
    detailPrice: FormControl<number | null>;
    detailWeight: FormControl<number | null>;
    detailUnitPrice: FormControl<number | null>;
    detailIsComplexPriceMode: FormControl<boolean>;
}

@Component({
    selector: 'app-transaction-form-component',
    templateUrl: './transaction-form-component.html',
    styleUrls: ['../../shared/components/form-style.scss', './transaction-form-component.scss'],
    imports: [ReactiveFormsModule, NgxsmkDatepickerComponent, SwitchComponent, TranslatePipe],
})
export class TransactionFormComponent implements OnChanges {
    private fb = inject(FormBuilder);
    private transactionService = inject(TransactionService);

    /**
     * Form disabled-e (pl.: töltődéskor)
     */
    @Input({ required: true }) isTransactionFormDisabled!: boolean;
    /**
     * Inputba kapott tranzakció (ha nem új tranzakcióról van szó)
     */
    @Input() transaction: TransactionDataFromBackend | null = null;

    /**
     * Mentés gombra kattintott a user
     */
    @Output() saved = new EventEmitter<NewTransaction>();
    /**
     * Tranzakció törlés gombra kattintott a user
     */
    @Output() deleted = new EventEmitter<number>();
    /**
     * Tranzakciós form
     */
    protected transactionForm: FormGroup;

    constructor() {
        this.transactionForm = this.buildForm({
            name: '',
            isIncome: false,
            isComplexTransaction: false,
            price: null,
            transactionDate: null,
            details: [],
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

            // Új form a frissen betöltött adatokkal
            this.transactionForm = this.buildForm(convertedInputValues);
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
        });
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
    }): FormGroup<DetailForm> {
        const detailGroup = this.fb.group({
            detailName: [params.name, Validators.required],
            detailPrice: [params.price, [Validators.min(1)]],
            detailWeight: [params.weight],
            detailUnitPrice: [params.unitPrice],
            detailIsComplexPriceMode: [params.isComplexPriceMode],
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
