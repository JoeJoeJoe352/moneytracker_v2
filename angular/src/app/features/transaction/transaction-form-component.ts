import { Component, EventEmitter, inject, Input, OnChanges, Output, signal, SimpleChanges } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { NgxsmkDatepickerComponent } from "ngxsmk-datepicker";
import { SwitchComponent } from "../../shared/components/switch.component";
import { validDate } from "./valid-date-validator";
import { TransactionService } from "./transaction-service";
import { Transaction, TransactionInput } from "./interfaces";

@Component({
    selector: "app-transaction-form-component",
    templateUrl: './transaction-form.html',
    imports: [
        ReactiveFormsModule, 
        NgxsmkDatepickerComponent,
        SwitchComponent,
    ],
    styleUrls: ["../../shared/components/form-style.scss"],
})
export class TransactionFormComponent implements OnChanges {
    private fb = inject(FormBuilder)
    private transactionService = inject(TransactionService)
    private defaultValues: TransactionInput = {
        name: '',
        isIncome: true,
        price: null,
        transactionDate: new Date(),
    }

    /**
     * Inputba kapott tranzakció (ha nem új tranzakcióról van szó)
     */
    @Input() transaction: Transaction | null = null;

    /**
     * Event, ha csak bezártuk a modalt
     */
    @Output() closeModal = new EventEmitter<void>();
    /**
     * Event, ha változott adat
     */
    @Output() dataChanged = new EventEmitter<void>();

    protected transactionForm: FormGroup
    protected isLoading = signal(false)

    constructor() {
        this.transactionForm = this.fb.nonNullable.group({
                name: [this.defaultValues.name, {
                    validators: [Validators.required, Validators.minLength(3), Validators.maxLength(200)],
                }],
                isIncome: new FormControl(this.defaultValues.isIncome),
                price: [this.defaultValues.price, [Validators.required, Validators.min(1)]],
                transactionDate: this.fb.control<Date | null>(this.defaultValues.transactionDate, {
                    validators: [Validators.required, validDate]
                })
            }
        )
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes["transaction"] && this.transaction !== null) {
            this.defaultValues = this.transactionService.convertDataToInput(this.transaction)
            this.transactionForm.patchValue(this.defaultValues)
        }
    }

    onSubmit() {
        if (this.transactionForm.invalid) {
            return;
        }
        this.isLoading.set(true)

        const raw = this.transactionForm.value;

        const payload = {
            ...raw,
            transactionDate: raw.transactionDate?.toISOString().slice(0, 10)
        };

        const transactionId = this.transaction?.id ?? null;

        this.transactionService.saveTransaction(payload, transactionId).subscribe({
            next: () => {
                this.isLoading.set(false);
                this.dataChanged.emit()
            },
            error: (response) => {
                console.error("unknown error during transaction creation!", response);
                this.isLoading.set(false);
            },
        })
    }

    // getters
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