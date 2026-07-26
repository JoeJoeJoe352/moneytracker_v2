import { Component, inject, signal } from '@angular/core';
import TransactionListComponent from '../transaction/transaction-list-component';
import { TransactionModalComponent } from '../transaction/transaction-modal';
import { NewTransaction, TransactionDataFromBackend } from '../transaction/interfaces';
import { TransactionService } from '../transaction/transaction-service';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NgxsmkDatepickerComponent } from 'ngxsmk-datepicker';
import { _, TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';

interface FilterFormInterface {
    name: FormControl<string>;
    date: FormControl<Date | null>;
}

@Component({
    selector: 'app-main-page-component',
    templateUrl: './transactions-page-component.html',
    styleUrl: './transactions-page-component.scss',
    standalone: true,
    imports: [
        TransactionModalComponent,
        TransactionListComponent,
        ReactiveFormsModule,
        NgxsmkDatepickerComponent,
    ],
})
export class TransactionsPage {
    private transactionService = inject(TransactionService);
    private fb = inject(FormBuilder);
    private router = inject(Router);
    private route = inject(ActivatedRoute);
    private translateService = inject(TranslateService);

    // Tranzakció lista adatok és betöltési állapot
    protected isTransactionListLoading = signal(false);
    protected transactionListData = signal<TransactionDataFromBackend[]>([]);
    // Tranzakció adatai, amit szerkeszteni szeretnénk. Adatok és betöltési állapot
    protected isTransactionDataLoading = signal(false);
    protected transactionData = signal<TransactionDataFromBackend | null>(null);
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

    protected isTransactionFormDisabled = signal(false);

    constructor() {
        const queryParams = this.route.snapshot.queryParams;
        let nameDefaultValue = '';
        let dateDefaultValue: Date | null = null;

        if (queryParams['name'] !== undefined) {
            nameDefaultValue = queryParams['name'];
        }
        if (queryParams['date'] !== undefined) {
            dateDefaultValue = new Date(queryParams['date']);
        }
        this.filterForm = this.fb.nonNullable.group({
            name: [nameDefaultValue],
            date: this.fb.control<Date | null>(dateDefaultValue),
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
    protected refreshAfterSave(): void {
        this.loadTransactionHistory();
        this.closeTransactionModal();
    }

    /**
     * keresési adatok resetelése
     */
    protected clearInputs(): void {
        this.filterForm.reset();
        // query paraméterek kiszedése az egy navigáció ugyanarra az url-re, csak queryk nélkül
        this.router.navigate([], {
            queryParams: {},
        });
    }

    /**
     * Feldob egy confirmot, hogy biztosan törölni szeretné-e a user a tranzakciót
     */
    popupDeletionConfirm(transactionId: number): void {
        if (confirm(this.translateService.instant(_('transaction.delete.confirm')))) {
            this.deleteTransaction(transactionId);
        }
    }

    /**
     * Tranzakció törlése a főoldalon
     */
    protected deleteTransaction(transactionId: number): void {
        this.runWithFormDisabled(this.transactionService.deleteTransaction(transactionId), () =>
            this.refreshAfterSave(),
        );
    }

    /**
     * Elment egy tranzakció adatait
     */
    saveTransaction(payload: NewTransaction): void {
        const transaction = this.transactionData();
        const obs =
            transaction !== null
                ? this.transactionService.updateTransaction(payload, transaction.id)
                : this.transactionService.saveTransaction(payload);

        this.runWithFormDisabled(obs, () => this.refreshAfterSave());
    }

    /**
     * Futtat egy api hívást és siker esetén meghív egy függvényt
     *
     * @param observable pl.: egy api hívás, ami observable-t ad vissza
     * @param onSuccess függvény, ami siker esetén lefut
     */
    private runWithFormDisabled<T>(observable: Observable<T>, onSuccess: () => void) {
        this.isTransactionFormDisabled.set(true);

        observable.subscribe({
            next: () => {
                this.isTransactionFormDisabled.set(false);
                onSuccess();
            },
            error: (response) => {
                console.error(
                    this.translateService.instant(_('transaction.delete.error')),
                    response,
                );
                this.isTransactionFormDisabled.set(false);
            },
        });
    }

    /**
     * Tranzakciók letöltése a backendről, a kártyás listához
     */
    loadTransactionHistory(): void {
        this.isTransactionListLoading.set(true);
        const params = new URLSearchParams({});

        const nameInputValue = this.filterForm.get(['name'])!.value as string;
        const dateInputValue = this.filterForm.get(['date'])!.value as Date | null;

        if (nameInputValue.trim() !== '') {
            params.append('name', nameInputValue);
        }

        if (dateInputValue !== null) {
            params.append('date', dateInputValue.toLocaleDateString());
        }

        this.transactionService.getTransactionHistory(params).subscribe({
            next: (response) => {
                this.isTransactionListLoading.set(false);
                this.transactionListData.set(response);
                // url beállítása
                this.router.navigate([], {
                    relativeTo: this.route,
                    queryParams: Object.fromEntries(params.entries()),
                    queryParamsHandling: 'merge',
                });
            },
            error: (response) => {
                console.error('unknown error during transaction creation!', response);
                this.isTransactionListLoading.set(false);
            },
        });
    }

    /**
     * Adott tranzakció letöltése a backendről, a szerkesztő formnak
     */
    protected loadTransaction(transactionId: number): void {
        this.isTransactionDataLoading.set(true);
        this.openTransactionModal();
        this.transactionService.getTransactionById(transactionId).subscribe({
            next: (response) => {
                this.isTransactionDataLoading.set(false);
                this.transactionData.set(response);
            },
            error: (response) => {
                console.error('unknown error during data loading!', response);
                this.isTransactionDataLoading.set(false);
            },
        });
    }
}
