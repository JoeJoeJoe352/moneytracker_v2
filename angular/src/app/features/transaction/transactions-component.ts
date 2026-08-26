import {
    Component,
    EventEmitter,
    inject,
    Input,
    OnChanges,
    OnInit,
    Output,
    signal,
    SimpleChanges,
} from '@angular/core';

import { TransactionModalComponent } from '../transaction/transaction-modal';
import { TransactionService } from '../transaction/transaction-service';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NgxsmkDatepickerComponent } from 'ngxsmk-datepicker';
import { TranslatePipe } from '@ngx-translate/core';
import { TransactionModalStateService } from '../transaction/transaction-modal-state-service';
import TransactionListComponent from '../transaction-list/transaction-list-component';
import { TransactionListElementData } from '../transaction-list/interfaces';

interface FilterFormInterface {
    name: FormControl<string>;
    date: FormControl<Date | null>;
}

interface FilterData {
    name: string;
    date: Date | null;
}

@Component({
    selector: 'app-transactions-list-component',
    templateUrl: './transactions-component.html',
    styleUrl: './transactions-component.scss',
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
export class TransactionsListComponent implements OnInit, OnChanges {
    @Input({ required: true }) isHistoryMode!: boolean;
    @Input({ required: true }) needSearchField!: boolean;
    /**
     * Ha ez az érték változik, a lista újratöltődik (pl. ha a szülő komponensben jött létre új tranzakció)
     */
    @Input() reloadTrigger = 0;

    /**
     * Akkor emitál, amikor a listában lévő valamelyik tranzakció változott (létrejött/módosult/törlődött),
     * hogy a szülő komponens is tudja frissíteni a saját adatait (pl. összesítés)
     */
    @Output() transactionsChanged = new EventEmitter<void>();

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
    protected transactionListData = signal<TransactionListElementData[]>([]);
    /**
     * Form definiciója
     */
    protected filterForm!: FormGroup<FilterFormInterface>;

    constructor() {
        const defaultData = this.getInitialDataFromQueryParams();
        this.buildForm(defaultData);

        // Mentés/törlés után újratöltjük a listát, és jelezzük a szülő komponensnek is
        this.modal.changed.subscribe(() => {
            this.loadTransactionHistory();
            this.transactionsChanged.emit();
        });
    }

    ngOnInit(): void {
        this.loadTransactionHistory();
    }

    ngOnChanges(changes: SimpleChanges): void {
        // A szülő komponensben létrejött új tranzakció után újratöltjük a listát
        if (changes['reloadTrigger'] && !changes['reloadTrigger'].firstChange) {
            this.loadTransactionHistory();
        }
    }

    /**
     * Query paraméterekből kiszedi a szűrőfeltételeket, amik be vannak állítva
     */
    private getInitialDataFromQueryParams(): FilterData {
        return {
            name: this.getQueryParam<string>('name') ?? '',
            date: this.getQueryParam<Date>('date', (v) => new Date(v)),
        };
    }

    /**
     * Filter formot létrehozza és beállítja az alapadatait
     */
    private buildForm(defaultData: FilterData): void {
        this.filterForm = this.fb.nonNullable.group({
            name: [defaultData.name],
            date: this.fb.control<Date | null>(defaultData.date),
        });
    }

    /**
     * Queryből lekéri a megfelelő kulcsú értéket
     * @param   key         kulcs, amit le akarunk kérni
     * @param   transform   az érték transformációja, ha nem stringben szeretnénk (opcionális)
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

        // query paraméterek kiszedése => egy navigáció ugyanarra az url-re, csak queryk nélkül
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

        const params = this.getValuesFromFilterInputs();

        if (!this.isHistoryMode) {
            this.transactionService.getLastTransactions().subscribe({
                next: (response) => {
                    this.isTransactionListLoading.set(false);
                    this.transactionListData.set(response);
                },
                error: (response) => {
                    console.error('unknown error during last transaction listing!', response);
                    this.isTransactionListLoading.set(false);
                },
            });
            return;
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
                console.error('unknown error during transaction history listing!', response);
                this.isTransactionListLoading.set(false);
            },
        });
    }

    /**
     * A filter inputok értékei alapján létrehoz egy URLSearchParams objektumot
     */
    private getValuesFromFilterInputs(): URLSearchParams {
        const params = new URLSearchParams({});
        const nameInputValue = this.filterForm.get(['name'])!.value.trim() as string;
        const dateInputValue = this.filterForm.get(['date'])!.value as Date | null;

        if (nameInputValue) {
            params.append('name', nameInputValue);
        }

        if (dateInputValue) {
            params.append('date', dateInputValue.toLocaleDateString('sv-SE'));
        }

        return params;
    }
}
