import { Component, EventEmitter, inject, Output } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { AuthService } from "../auth/auth-service";
import { NgxsmkDatepickerComponent } from "ngxsmk-datepicker";
import { SwitchComponent } from "../../shared/components/switch.component";

@Component({
    selector: "app-transaction-form-component",
    templateUrl: './transaction-form.html',
    imports: [
        ReactiveFormsModule, 
        NgxsmkDatepickerComponent,
        SwitchComponent,
    ],
    providers: [],
    styleUrls: ["../../shared/components/form-style.scss", "./transaction-form.scss"],
})
export class TransactionFormComponent {
    @Output() closeModal = new EventEmitter<void>();

    // csak komponensben elérhető változók  
    private fb = inject(FormBuilder)
    private authService = inject(AuthService)

    protected transactionForm: FormGroup

    constructor() {
        this.transactionForm = this.fb.nonNullable.group(
            {
                name: ['', {
                    validators: [Validators.required, Validators.minLength(3), Validators.maxLength(200)],
                }],
                transactionType: new FormControl(true),
                price: [0, [Validators.required, Validators.min(1)]],
                transactionDate: new FormControl<Date | null>(null),
            }
        )
    }

    isLoading() {
        return 0
    }

    onSubmit() {
        const raw = this.transactionForm.value;

        const payload = {
        ...raw,
        transactionDate: raw.transactionDate?.toISOString().slice(0, 10)
        };
        console.log('qwe')
    }
}