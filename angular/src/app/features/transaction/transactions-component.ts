import {
    Component,
    DestroyRef,
    EventEmitter,
    inject,
    Input,
    OnChanges,
    OnInit,
    Output,
    signal,
    SimpleChanges,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TransactionModalComponent } from '../transaction/transaction-modal';
import { TransactionService } from '../transaction/transaction-service';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { TransactionModalStateService } from '../transaction/transaction-modal-state-service';
import TransactionListComponent from '../transaction-list/transaction-list-component';
import { TransactionListElementData } from '../transaction-list/interfaces';
import { TransactionFilter } from './transaction-filter-component';

export interface FilterData {
    name: string;
    date: Date | null;
}

@Component({
    selector: 'app-transactions-list-component',
    templateUrl: './transactions-component.html',
    standalone: true,
    imports: [
        TransactionModalComponent,
        TransactionListComponent,
        TranslatePipe,
        TransactionFilter,
    ],
    providers: [TransactionModalStateService],
})
export class TransactionsListComponent implements OnInit, OnChanges {
    private transactionService = inject(TransactionService);
    private router = inject(Router);
    private route = inject(ActivatedRoute);
    private destroyRef = inject(DestroyRef);
    protected modal = inject(TransactionModalStateService);

    /**
     * Teljes listát szeretnénk-e látni, vagy csak egy részét
     */
    @Input({ required: true }) isHistoryMode!: boolean;
    /**
     * Megjelenjen-e a keresési mező
     */
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

    /**
     * Töltődik-e jelenleg a tranzakciós lista
     */
    protected isTransactionListLoading = signal(true);
    /**
     * Tranzakciós lista
     */
    protected transactionListData = signal<TransactionListElementData[]>([]);
    /**
     * Kezdeti szűrőfeltételek a query paraméterekből, a szűrő komponens inicializálásához
     */
    protected dataFromQuery = signal<FilterData | null>(null);

    /**
     * Legutóbb kapott (a route query paramétereiből számított) szűrőparaméterek, amikor nem a
     * szűrő form küldi az újratöltést kiváltó eseményt (pl. reloadTrigger, modal mentés/törlés)
     */
    private latestParams = new URLSearchParams();

    ngOnInit(): void {
        // A route query változára  újratöltjük a listát
        this.route.queryParams
            .pipe(takeUntilDestroyed(this.destroyRef)) // amíg a komponens meg nem szűnik
            .subscribe((queryParams) => {
                this.latestParams = new URLSearchParams(queryParams as Params);
                // A kezdeti értékből állítjuk be a szűrő form kezdőértékeit
                if (this.dataFromQuery() === null) {
                    this.dataFromQuery.set(this.toFilterData(this.latestParams));
                }
                this.loadTransactionHistory();
            });

        // Mentés/törlés után újratöltjük a listát, és jelezzük a szülő komponensnek is
        this.modal.changed.subscribe(() => {
            this.loadTransactionHistory();
            this.transactionsChanged.emit();
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        // A szülő komponensben létrejött új tranzakció után újratöltjük a listát
        if (changes['reloadTrigger'] && !changes['reloadTrigger'].firstChange) {
            this.loadTransactionHistory();
        }
    }

    /**
     * Szűrő form beküldésekor lefutó művelet: a route query paramétereinek frissítése, amire a
     * queryParams feliratkozás reagálva újratölti a listát
     */
    protected onFilterSubmit(data: FilterData): void {
        this.router.navigate([], {
            relativeTo: this.route,
            queryParams: this.toQueryParams(data),
        });
    }

    /**
     * keresési adatok resetelése
     */
    protected clearInputs(): void {
        this.router.navigate([], {
            relativeTo: this.route,
            queryParams: {},
        });
    }

    /**
     * Tranzakciók letöltése a backendről, a kártyás listához
     */
    loadTransactionHistory(): void {
        this.isTransactionListLoading.set(true);

        const apiObserver = this.isHistoryMode
            ? this.transactionService.getTransactionHistory(this.latestParams)
            : this.transactionService.getLastTransactions();

        apiObserver.subscribe({
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

    /**
     * URLSearchParams-ból kiszedi a szűrőfeltételeket, amik be vannak állítva
     */
    private toFilterData(params: URLSearchParams): FilterData {
        const date = params.get('date');

        return {
            name: params.get('name') ?? '',
            date: date ? new Date(date) : null,
        };
    }

    /**
     * Szűrőfeltételekből létrehoz egy, a router.navigate queryParams opciójának megfelelő objektumot
     */
    private toQueryParams(data: FilterData): Params {
        const params: Record<string, string> = {};

        if (data.name) {
            params['name'] = data.name;
        }

        if (data.date) {
            params['date'] = data.date.toLocaleDateString('sv-SE');
        }

        return params;
    }
}
