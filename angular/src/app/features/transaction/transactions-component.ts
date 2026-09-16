import {
    Component,
    DestroyRef,
    EventEmitter,
    inject,
    Input,
    OnChanges,
    OnInit,
    Output,
    resource,
    signal,
    SimpleChanges,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TransactionService } from '../transaction/transaction-service';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { TransactionModalStateService } from '../transaction/transaction-modal-state-service';
import TransactionListComponent from '../transaction-list/transaction-list-component';
import { TransactionFilter } from './transaction-filter-component';
import { MatCardModule } from '@angular/material/card';
import { firstValueFrom } from 'rxjs';

export interface FilterData {
    name: string;
    date: Date | null;
}

@Component({
    selector: 'app-transactions-list-component',
    templateUrl: './transactions-component.html',
    styleUrl: './transactions-component.scss',
    standalone: true,
    imports: [TransactionListComponent, TranslatePipe, TransactionFilter, MatCardModule],
    providers: [TransactionModalStateService],
})
export class TransactionsListComponent implements OnInit, OnChanges {
    private readonly transactionService = inject(TransactionService);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly destroyRef = inject(DestroyRef);
    protected readonly modal = inject(TransactionModalStateService);

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
     * Kezdeti szűrőfeltételek a query paraméterekből, a szűrő komponens inicializálásához
     */
    protected filterInputDefaultValuesFromQuery = signal<FilterData | null>(null);

    /**
     * Legutóbb kapott (a route query paramétereiből számított) szűrőparaméterek, amikor nem a
     * szűrő form küldi az újratöltést kiváltó eseményt (pl. reloadTrigger, modal mentés/törlés)
     */
    private latestParams = signal(new URLSearchParams());

    ngOnInit(): void {
        // A route query változára újratöltjük a listát
        this.route.queryParams
            .pipe(takeUntilDestroyed(this.destroyRef)) // amíg a komponens meg nem szűnik
            .subscribe((queryParams) => {
                // latestParams beállítás után automatikusan újratöltődik a lista
                this.latestParams.set(new URLSearchParams(queryParams as Params));
                // A kezdeti értékből állítjuk be a szűrő form kezdőértékeit
                if (this.filterInputDefaultValuesFromQuery() === null) {
                    this.filterInputDefaultValuesFromQuery.set(this.toFilterData(this.latestParams()));
                }
            });

        // Mentés/törlés után újratöltjük a listát, és jelezzük a szülő komponensnek is
        this.modal.changed.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
            this.transactionListData.reload();
            this.transactionsChanged.emit();
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        // A szülő komponens ha üzen (új tranzakció felvételéről), akkor újratöltjük a listát
        if (changes['reloadTrigger'] && !changes['reloadTrigger'].firstChange) {
            this.transactionListData.reload();
        }
    }

    /**
     * Tranzakciós lista adatok. A query paraméterek változtatásakor magától betöltődik
     */
    protected transactionListData = resource({
        defaultValue: [],
        params: this.latestParams,
        loader: async ({params}) => {
            const apiObserver = this.isHistoryMode
                ? this.transactionService.getTransactionHistory(params)
                : this.transactionService.getLastTransactions();

            return firstValueFrom(apiObserver);
        },
    });

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
