import { Component, EventEmitter, inject, Input, Output, signal } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { NgxsmkDatepickerComponent } from "ngxsmk-datepicker";
import { SwitchComponent } from "../../shared/components/switch.component";
import { validDate } from "./valid-date-validator";
import { TransactionService } from "./transaction-service";
import { Transaction } from "./interfaces";

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
export class TransactionFormComponent {
    @Input() transaction: Transaction | null = null;

    @Output() closeModal = new EventEmitter<void>();

    private fb = inject(FormBuilder)
    private transactionService = inject(TransactionService)

    protected transactionForm: FormGroup
    protected isLoading = signal(false)

    constructor() {
        this.transactionForm = this.fb.nonNullable.group(
            {
                name: ['', {
                    validators: [Validators.required, Validators.minLength(3), Validators.maxLength(200)],
                }],
                isIncome: new FormControl(true),
                price: [null, [Validators.required, Validators.min(1)]],
                transactionDate: [new FormControl<Date | null>(null), {
                    validators: [Validators.required, validDate]
                }],
            }
        )
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

        this.transactionService.saveTransaction(payload).subscribe({
            next: () => {
                this.isLoading.set(false);
                // Frissíteni kell minden listát, ahol ezek a tranzakciók megjelennek
                this.transactionService.notifyRefresh();
                this.closeModal.emit()
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