import { Component, computed, inject, OnInit, signal } from '@angular/core';
import TransactionListComponent from '../transaction/transaction-list-component';
import { TransactionModalComponent } from '../transaction/transaction-modal';
import { NewTransaction, TransactionDataFromBackend } from '../transaction/interfaces';
import { TransactionService } from '../transaction/transaction-service';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NgxsmkDatepickerComponent } from 'ngxsmk-datepicker';
import { TranslatePipe } from '@ngx-translate/core';
import { of, switchMap } from 'rxjs';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { TransactionActionService } from '../transaction/transaction-action-service';

interface FilterFormInterface {
    name: FormControl<string>;
    date: FormControl<Date | null>;
}

@Component({
    selector: 'app-transaction-page-component',
    templateUrl: './transactions-page-component.html',
    styleUrl: './transactions-page-component.scss',
    standalone: true,
    imports: [
        TransactionModalComponent,
        TransactionListComponent,
        ReactiveFormsModule,
        NgxsmkDatepickerComponent,
        TranslatePipe,
    ],
})
export class TransactionsPage implements OnInit {
    private transactionService = inject(TransactionService);
    private fb = inject(FormBuilder);
    private router = inject(Router);
    private route = inject(ActivatedRoute);
    private transactionActionService = inject(TransactionActionService);

    /**
     * Töltődik-e jelenleg a tranzakciós lista
     */
    protected isTransactionListLoading = signal(true);
    /**
     * Tranzakció létrehozó modal bezárása
     */
    protected isTransactionModalOpen = signal(false);
    /**
     * Tranzakciós lista
     */
    protected transactionListData = signal<TransactionDataFromBackend[]>([]);
    /**
     * Form disabled-e
     */
    protected isTransactionFormDisabled = signal(false);
    /**
     * Kiválasztott tranzakció azonosítója.
     * Ha ez változik, akkor le fog futni a tranzakció betöltés is (transactionData)
     */
    protected selectedTransactionIdTrigger = signal<number | null>(null);
    /**
     * Kiválasztott tranzakció adatai
     */
    protected transactionData = toSignal(
        toObservable(this.selectedTransactionIdTrigger).pipe(
            switchMap((id) =>
                id === null ? of(null) : this.transactionService.getTransactionById(id),
            ),
        ),
        { initialValue: null },
    );
    /**
     *  Betöltés alatt van-e a tranzakció
     */
    protected isTransactionDataLoading = computed(
        () => this.selectedTransactionIdTrigger() !== null && this.transactionData() === null,
    );
    /**
     * Form definiciója
     */
    protected filterForm!: FormGroup<FilterFormInterface>;

    constructor() {
        const nameDefaultValue = this.getQueryParam<string>('name') ?? '';
        const dateDefaultValue = this.getQueryParam<Date>('date', (v) => new Date(v));

        this.filterForm = this.fb.nonNullable.group({
            name: [nameDefaultValue],
            date: this.fb.control<Date | null>(dateDefaultValue),
        });
    }

    ngOnInit(): void {
        this.loadTransactionHistory();
    }

    /**
     * Queryből lekéri a megfelelő kulcsú értéket
     * @param   key         kulcs, amit le akarunk kérni
     * @param   transform   az érték utólagos átalakítása
     * @returns T
     */
    private getQueryParam<T>(key: string, transform?: (queryValue: string) => T): T | null {
        const value = this.route.snapshot.queryParams[key];
        return value !== undefined ? (transform ? transform(value) : (value as T)) : null;
    }

    /**
     * Tranzakció létrehozó modal bezárása, ha változott az adat
     */
    protected refreshAfterSave(): void {
        this.loadTransactionHistory();
        this.closeTransactionModal();
    }

    /**
     * Tranzakció betöltése szerkesztéshez
     */
    protected openTransactionModal(id: number | null): void {
        this.selectedTransactionIdTrigger.set(id);
        this.isTransactionModalOpen.set(true);
    }

    /**
     * Tranzakció létrehozó modal becsukása
     */
    protected closeTransactionModal(): void {
        this.selectedTransactionIdTrigger.set(null);
        this.isTransactionModalOpen.set(false);
    }

    /**
     * keresési adatok resetelése
     */
    protected clearInputs(): void {
        this.filterForm.reset();
        // query paraméterek kiszedése = egy navigáció ugyanarra az url-re, csak queryk nélkül
        this.router.navigate([], {
            queryParams: {},
        });
        this.loadTransactionHistory();
    }

    /**
     * Tranzakció törlése a főoldalon
     */
    protected popupDeletionConfirm(transactionId: number): void {
        if (this.transactionActionService.confirmDeletion()) {
            this.transactionActionService.deleteTransaction(
                transactionId,
                this.isTransactionFormDisabled,
                () => this.refreshAfterSave(),
            );
        }
    }

    /**
     * Elmenti egy tranzakció adatait
     */
    protected saveTransaction(payload: NewTransaction): void {
        const transactionData = this.transactionData();
        this.transactionActionService.saveTransaction(
            payload,
            transactionData?.id ?? null,
            this.isTransactionFormDisabled,
            () => this.refreshAfterSave(),
        );
    }

    /**
     * Tranzakciók letöltése a backendről, a kártyás listához
     */
    loadTransactionHistory(): void {
        this.isTransactionListLoading.set(true);

        const params = new URLSearchParams({});
        const nameInputValue = this.filterForm.get(['name'])!.value.trim() as string;
        const dateInputValue = this.filterForm.get(['date'])!.value as Date | null;

        if (nameInputValue.trim()) {
            params.append('name', nameInputValue);
        }

        if (dateInputValue) {
            params.append('date', dateInputValue.toISOString().split('T')[0]);
        }

        this.transactionService.getTransactionHistory(params).subscribe({
            next: (response) => {
                this.isTransactionListLoading.set(false);
                this.transactionListData.set(response);

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
}
