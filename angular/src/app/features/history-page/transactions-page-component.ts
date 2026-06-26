import { Component, inject, signal } from "@angular/core";
import TransactionListComponent from "../transaction/transaction-list-component";
import { TransactionModalComponent } from "../transaction/transaction-modal";
import { Transaction } from "../transaction/interfaces";
import { TransactionService } from "../transaction/transaction-service";
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { NgxsmkDatepickerComponent } from "ngxsmk-datepicker";

interface FilterFormInterface {
    name: FormControl<string>,
    date: FormControl<Date|null>,
}

@Component({
    selector: "app-main-page-component",
    templateUrl: "./transactions-page-component.html",
    styleUrl: "./transactions-page-component.scss",
    standalone: true,
    imports: [TransactionModalComponent, TransactionListComponent, ReactiveFormsModule, NgxsmkDatepickerComponent],
})
export class TransactionsPage {
    private transactionService = inject(TransactionService);
    private fb = inject(FormBuilder)
    private router = inject(Router)
    private route = inject(ActivatedRoute)

    // Tranzakció lista adatok és betöltési állapot
    protected isTransactionListLoading = signal(false)
    protected transactionListData = signal<Transaction[]>([])
    // Tranzakció adatai, amit szerkeszteni szeretnénk. Adatok és betöltési állapot
    protected isTransactionDataLoading = signal(false);
    protected transactionData = signal<Transaction|null>(null);
    /**
     * Tranzakció létrehozó modal bezárása
     */
    protected isNewTransactionModalOpen = signal(false);
    /**
     * Tranzakció létrehozó modal felnyitása
     */
    protected openTransactionModal(): void {
        this.isNewTransactionModalOpen.set(true);
    }
    protected filterForm!: FormGroup<FilterFormInterface>;
    
    constructor() {
        this.filterForm = this.fb.nonNullable.group({
            name: [''],
            date: this.fb.control<Date | null>(null)
        });
    }

    /**
     * Tranzakció létrehozó modal becsukása
     */
    protected closeTransactionModal(): void {
        this.transactionData.set(null);
        this.isNewTransactionModalOpen.set(false);
    }

    /**
     * Tranzakció létrehozó modal bezárása, ha változott az adat 
     */
    protected handleMondalDataChange(): void {
        this.loadTransactionHistory();
        this.closeTransactionModal();
    }

    protected clearInputs(): void {
        this.filterForm.reset()
    }

    /**
     * Tranzakciók letöltése a backendről, a kártyás listához
     */
    loadTransactionHistory(): void {
        this.isTransactionListLoading.set(true);
        const params = new URLSearchParams({});

        const nameInputValue = this.filterForm.get(['name'])!.value as string
        const dateInputValue = this.filterForm.get(['date'])!.value as Date|null

        if (nameInputValue.trim() !== '') {
            params.append('name', nameInputValue)
        }

        if (dateInputValue !== null) {
            params.append('date', dateInputValue.toLocaleDateString())
        }

        this.transactionService.getTransactionHistory(params).subscribe({
            next: (response) => {
                this.isTransactionListLoading.set(false);
                this.transactionListData.set(response)
                // url beállítása
                this.router.navigate([], {
                    relativeTo: this.route,
                    queryParams: Object.fromEntries(params.entries()),
                    queryParamsHandling: 'merge',
                });
            },
            error: (response) => {
                console.error("unknown error during transaction creation!", response);
                this.isTransactionListLoading.set(false);
            },
        })
    }

    /**
     * Adott tranzakció letöltése a backendről, a szerkesztő formnak
     */
    protected loadTransaction(transactionId: number): void {
        this.isTransactionDataLoading.set(true)
        this.transactionService.getTransactionById(transactionId).subscribe({
            next: (response) => {
                this.isTransactionDataLoading.set(false);
                this.transactionData.set(response);
                this.openTransactionModal();
            },
            error: (response) => {
                console.error("unknown error during data loading!", response);
                this.isTransactionDataLoading.set(false);
            },
        })
    }
}