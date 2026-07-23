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
import { NewTransaction, TransactionDataFromBackend } from './interfaces';
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

    /**
     * Inputba kapott tranzakció (ha nem új tranzakcióról van szó)
     */
    @Input() transaction: TransactionDataFromBackend | null = null;
    /**
     * Event, ha csak bezártuk a modalt
     */
    @Output() closeModal = new EventEmitter<void>();
    /**
     * Event, ha változott adat
     */
    @Output() dataChanged = new EventEmitter<void>();

    @Output() saved = new EventEmitter<NewTransaction>();
    @Output() deleted = new EventEmitter<number>();

    /**
     * Tranzakciós form
     */
    protected transactionForm: FormGroup;
    /**
     * Api válaszra várunk-e (mentés, adatbetöltés)
     */
    protected isLoading = signal(false);

    protected showDetailsToggleIsOn = signal(false); 

    constructor() {
        this.transactionForm = this.fb.nonNullable.group({
            name: [
                '',
                {
                    validators: [
                        Validators.required,
                        Validators.minLength(3),
                        Validators.maxLength(200),
                    ],
                },
            ],
            isIncome: new FormControl(false),
            price: [null, [Validators.required, Validators.min(1)]],
            transactionDate: this.fb.control<Date | null>(null, {
                validators: [Validators.required, validDate],
            }),
            details: this.fb.array<FormGroup<DetailForm>>([this.generateNewRow()]),
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
            this.transactionForm.patchValue(convertedInputValues);

            // betöltés után töröljük a form detail elemeit és beállítjuk a backendről kapottakat
            this.details.clear();
            convertedInputValues.transactionDetails.forEach((d) => {
                this.details.push(
                    this.fb.group({
                        detailName: new FormControl(d.name, { validators: Validators.required }),
                        detailPrice: new FormControl(d.price, { validators: [Validators.min(1)] }),
                        detailWeight: new FormControl(d.weight),
                        detailUnitPrice: new FormControl(d.unitPrice),
                    }) as FormGroup<DetailForm>,
                );
            });
        }
    }

    /**
     * Form elküldésekori műveletek
     */
    onSubmit(): void {
        if (this.transactionForm.invalid) {
            return;
        }
        this.isLoading.set(true);

        const payload = this.transactionForm.value;

        if (this.isExistingTransaction()) {
            const transactionId = this.transaction.id;
            this.transactionService.updateTransaction(payload, transactionId).subscribe({
                next: () => {
                    this.isLoading.set(false);
                    this.dataChanged.emit();
                },
                error: (response) => {
                    console.error(
                        this.translateService.instant(_('transaction.update.error')),
                        response,
                    );
                    this.isLoading.set(false);
                },
            });
        } else {
            this.transactionService.saveTransaction(payload).subscribe({
                next: () => {
                    this.isLoading.set(false);
                    this.dataChanged.emit();
                },
                error: (response) => {
                    console.error(
                        this.translateService.instant(_('transaction.create.error')),
                        response,
                    );
                    this.isLoading.set(false);
                },
            });
        }
    }

    /**
     * Feldob egy confirmot, hogy biztosan törölni szeretné-e a user a confirmot
     */
    popupDeletionConfirm(): void {
        // todo kiszervezni szülő komponensbe
        if (confirm(this.translateService.instant(_('transaction.delete.confirm')))) {
            this.deleteTransaction();
        }
    }

    /**
     * Tranzakció törlése
     */
    deleteTransaction() {
        if (!this.isExistingTransaction()) {
            console.error('Cannot delete new transaction. Error!');
            return;
        }
        this.deleted.emit(this.transaction.id);
        // todo kiszervezni szülő komponensbe a törlés logikát, isLoading az legyen paraméter
        this.isLoading.set(true);
        this.transactionService.deleteTransaction(this.transaction.id).subscribe({
            next: () => {
                this.isLoading.set(false);
                this.dataChanged.emit();
            },
            error: (response) => {
                console.error(
                    this.translateService.instant(_('transaction.delete.error')),
                    response,
                );
                this.isLoading.set(false);
            },
        });
    }

    deleteRow(index: number) {
        this.details.removeAt(index);
    }

    addRow() {
        if (!this.transactionForm.valid) {
            console.error('form is invalid');
            console.error(this.transactionForm.errors);
            return;
        }
        this.details.push(this.generateNewRow());
    }

    /**
     * alapértelmezett detail, amit új tranzakciónál (mert mindig kell lennie egy detailnak, csak nem látszik a usernek esetleg),
     * vagy új detail hozzáadásánál bővítjük vele a formot
     * Fontos, hogy mindig új példányt hozzunk létre, mert ha változóba lenne kirakva, akkor ugyanazt a példányt adná át
     * és ugyanaz az érték íródna be a form elemekbe
     */
    generateNewRow(): FormGroup<DetailForm> {
        return this.fb.group({
            detailName: new FormControl(''),
            detailPrice: new FormControl<number | null>(null, {
                validators: [Validators.min(1)],
            }),
            detailWeight: new FormControl<number | null>(null),
            detailUnitPrice: new FormControl<number | null>(null),
        }) as FormGroup<DetailForm>;
    }

    /**
     * Létező tranzakció adatai vannak-e a formban
     */
    isExistingTransaction(): this is { transaction: TransactionDataFromBackend } {
        return this.transaction !== null;
    }

    get name(): FormControl<string> {
        return this.transactionForm.get('name') as FormControl<string>;
    }

    get price(): FormControl<string> {
        return this.transactionForm.get('price') as FormControl<string>;
    }

    get transactionDate(): FormControl<string> {
        return this.transactionForm.get('transactionDate') as FormControl<string>;
    }

    get isIncome(): FormControl<boolean> {
        return this.transactionForm.get('isIncome') as FormControl<boolean>;
    }

    get details(): FormArray<FormGroup<DetailForm>> {
        return this.transactionForm.get('details') as FormArray<FormGroup<DetailForm>>;
    }
}
