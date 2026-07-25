import {
    Component,
    EventEmitter,
    inject,
    Input,
    OnChanges,
    Output,
    signal,
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
import { _, TranslatePipe, TranslateService } from '@ngx-translate/core';

/**
 * Detail form elemei
 */
interface DetailForm {
    detailName: FormControl<string>;
    detailPrice: FormControl<number | null>;
    detailWeight: FormControl<number | null>;
    detailUnitPrice: FormControl<number | null>;
}

@Component({
    selector: 'app-transaction-form-component',
    templateUrl: './transaction-form-component.html',
    styleUrls: ['../../shared/components/form-style.scss'],
    imports: [ReactiveFormsModule, NgxsmkDatepickerComponent, SwitchComponent, TranslatePipe],
})
export class TransactionFormComponent implements OnChanges {
    private translateService = inject(TranslateService);
    private fb = inject(FormBuilder);
    private transactionService = inject(TransactionService);

    @Input({ required: true }) isTransactionFormDisabled!: boolean;
    /**
     * Inputba kapott tranzakció (ha nem új tranzakcióról van szó)
     */
    @Input() transaction: TransactionDataFromBackend | null = null;
    /**
     * Event, ha a bezárás gombra kattintott a user
     */
    @Output() closeModal = new EventEmitter<void>();
    /**
     * Event, ha változott adat
     */
    @Output() dataChanged = new EventEmitter<void>();
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
    /**
     * Tranzakciós részletek mutatása kapcsoló állása benyomott-e
     */
    protected showDetailsToggleIsOn = signal(false);

    constructor() {
        this.transactionForm = this.buildForm({
            name: '',
            isIncome: false,
            price: null,
            transactionDate: null,
            details: [],
        });
    }

    /**
     * @param changes Betöltés után ha van kezdőérték beállítva, akkor a formba azokat állítjuk be
     */
    ngOnChanges(changes: SimpleChanges): void {
        if (changes['transaction'] && this.transaction !== null) {
            const convertedInputValues = this.transactionService.utils.convertDataToInput(
                this.transaction,
            );
            this.showDetailsToggleIsOn.set(this.transaction.isComplexTransaction);

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
            price: [defaults.price, { validators: [Validators.min(1)] }],
            transactionDate: this.fb.control(defaults.transactionDate, {
                validators: [Validators.required, validDate],
            }),
            details: this.fb.array(defaults.details.map((detail) => this.generateNewRow(detail))),
        });
    }

    /**
     * Részletek megjelenítése/elrejtése kapcsoló átállítás
     */
    onSwitchDetailToggle() {
        if (!this.showDetailsToggleIsOn()) {
            this.showDetailsToggleIsOn.set(true);
            return;
        }
        
        if (
            this.details.length === 0 ||
            confirm(this.translateService.instant(_('transaction.detail.toggle.confirm')))
        ) {
            this.showDetailsToggleIsOn.set(false);
            this.details.clear();
        }
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
        if (this.details.length === 1) {
            console.error('utolsó sort nem lehet törölni');
            //todo ez legyen majd toast
            alert(this.translateService.instant(_('transaction.detail.delete.last-one-error')));

            return;
        }
        this.details.removeAt(index);
    }

    /**
     * Létrehoz egy új üres sort
     */
    addRow(): void {
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
    }): FormGroup<DetailForm> {
        return this.fb.group({
            detailName: [params.name, Validators.required],
            detailPrice: [params.price, [Validators.min(1), Validators.required]],
            detailWeight: [params.weight],
            detailUnitPrice: [params.unitPrice],
        }) as FormGroup<DetailForm>;
    }

    /**
     * Generál egy új input sort, üres adatokkal
     */
    generateNewEmptyRow() {
        return this.generateNewRow({ name: '', price: null, unitPrice: null, weight: null });
    }

    /**
     * Létező tranzakció adatai vannak-e a formban (+ guard)
     */
    isExistingTransaction(): this is { transaction: TransactionDataFromBackend } {
        return this.transaction !== null;
    }

    // Getters

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

    get details(): FormArray<FormGroup<DetailForm>> {
        return this.transactionForm.get('details') as FormArray<FormGroup<DetailForm>>;
    }
}
