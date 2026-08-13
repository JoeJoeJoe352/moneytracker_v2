import { Component, inject, OnInit, signal } from '@angular/core';
import TransactionListComponent from '../transaction/transaction-list-component';
import { TransactionModalComponent } from '../transaction/transaction-modal';
import { TransactionDataFromBackend } from '../transaction/interfaces';
import { TransactionService } from '../transaction/transaction-service';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NgxsmkDatepickerComponent } from 'ngxsmk-datepicker';
import { TranslatePipe } from '@ngx-translate/core';
import { TransactionModalStateService } from '../transaction/transaction-modal-state-service';

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
    providers: [TransactionModalStateService],
})
export class TransactionsPage implements OnInit {
    private transactionService = inject(TransactionService);
    private fb = inject(FormBuilder);
    private router = inject(Router);
    private route = inject(ActivatedRoute);
    protected modal = inject(TransactionModalStateService);

    /**
     * Töltődik-e jelenleg a tranzakciós lista
     */
    protected isTransactionListLoading = signal(true);
    /**
     * Tranzakciós lista
     */
    protected transactionListData = signal<TransactionDataFromBackend[]>([]);
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

        // Mentés/törlés után újratöltjük a listát
        this.modal.changed.subscribe(() => this.loadTransactionHistory());
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
            params.append('date', dateInputValue.toLocaleDateString('sv-SE'));
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
