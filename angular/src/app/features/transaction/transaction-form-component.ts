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
import { TransactionDataFromBackend, TransactionInputDefaultValues } from './interfaces';
import { _, TranslatePipe, TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'app-transaction-form-component',
    templateUrl: './transaction-form-component.html',
    styleUrls: ['../../shared/components/form-style.scss'],
    imports: [ReactiveFormsModule, NgxsmkDatepickerComponent, SwitchComponent, TranslatePipe],
})
export class TransactionFormComponent implements OnChanges {
    private translateService = inject(TranslateService)
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

    private fb = inject(FormBuilder);
    private transactionService = inject(TransactionService);
    private defaultValues: TransactionInputDefaultValues = {
        name: '',
        isIncome: true,
        price: null,
        transactionDate: new Date(),
    };

    protected transactionForm: FormGroup;
    protected isLoading = signal(false);

    constructor() {
        this.transactionForm = this.fb.nonNullable.group({
            name: [
                this.defaultValues.name,
                {
                    validators: [
                        Validators.required,
                        Validators.minLength(3),
                        Validators.maxLength(200),
                    ],
                },
            ],
            isIncome: new FormControl(this.defaultValues.isIncome),
            price: [this.defaultValues.price, [Validators.required, Validators.min(1)]],
            transactionDate: this.fb.control<Date | null>(this.defaultValues.transactionDate, {
                validators: [Validators.required, validDate],
            }),
        });
    }

    /**
     * @param changes Betöltés után ha van kezdőérték beállítva, akkor a formba azokat állítjuk be
     */
    ngOnChanges(changes: SimpleChanges): void {
        if (changes['transaction'] && this.transaction !== null) {
            this.defaultValues = this.transactionService.utils.convertDataToInput(this.transaction);
            this.transactionForm.patchValue(this.defaultValues);
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
                    console.error(this.translateService.instant(_("transaction.update.error")), response);
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
                    console.error(this.translateService.instant(_("transaction.create.error")), response);
                    this.isLoading.set(false);
                },
            });
        }
    }

    /**
     * Feldob egy confirmot, hogy biztosan törölni szeretné-e a user a confirmot
     */
    popupDeletionConfirm(): void {
        if (confirm(this.translateService.instant(_("transaction.delete.confirm")))) {
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
        this.isLoading.set(true);
        this.transactionService.deleteTransaction(this.transaction.id).subscribe({
            next: () => {
                this.isLoading.set(false);
                this.dataChanged.emit();
            },
            error: (response) => {
                console.error(this.translateService.instant(_("transaction.delete.error")), response);
                this.isLoading.set(false);
            },
        });
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
}
