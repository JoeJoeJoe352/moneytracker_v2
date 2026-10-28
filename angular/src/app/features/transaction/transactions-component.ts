import {
    Component,
    computed,
    effect,
    inject,
    input,
    linkedSignal,
    OnChanges,
    output,
    resource,
    SimpleChanges,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { TransactionService } from '../transaction/transaction-service';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { TransactionModalStateService } from '../transaction/transaction-modal-state-service';
import TransactionListComponent from './transaction-list/transaction-list-component';
import { TransactionFilter } from './transaction-filter-component';
import { MatCardModule } from '@angular/material/card';
import { firstValueFrom, map } from 'rxjs';
import { TransactionBarChartComponent } from '../statistics/transaction-bar-chart-component';
import { TimeHelper } from '@app/shared/services/time-helper';
import { TransactionHistoryInterface } from './interfaces';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';

export interface FilterData {
    transactionId: number | null;
    name: string;
    date: Date | null;
}

export interface SearchDataInterface extends FilterData {
    pageNumber: number | null;
}

@Component({
    selector: 'app-transactions-list-component',
    templateUrl: './transactions-component.html',
    styleUrl: './transactions-component.scss',
    imports: [
        TransactionListComponent,
        TranslatePipe,
        TransactionFilter,
        MatCardModule,
        TransactionBarChartComponent,
        MatPaginatorModule,
    ],
    providers: [TransactionModalStateService],
})
export class TransactionsListComponent implements OnChanges {
    private readonly transactionService = inject(TransactionService);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly timeHelper = inject(TimeHelper);
    protected readonly modal = inject(TransactionModalStateService);
    /**
     * Teljes listát szeretnénk-e látni, vagy csak egy részét
     */
    public isHistoryMode = input.required<boolean>();
    /**
     * Megjelenjen-e a keresési mező
     */
    public needSearchField = input.required<boolean>();
    /**
     * Ha ez az érték változik, a lista újratöltődik (pl. ha a szülő komponensben jött létre új tranzakció)
     */
    public reloadTrigger = input<number>(0);
    /**
     * Akkor emitál, amikor a listában lévő valamelyik tranzakció változott (létrejött/módosult/törlődött),
     * hogy a szülő komponens is tudja frissíteni a saját adatait (pl. összesítés)
     */
    public transactionsChanged = output<void>();

    /**
     * A route aktuális query paraméterei. Változáskor a lista magától újratöltődik
     */
    private readonly latestParams = toSignal(
        this.route.queryParams.pipe(map((params) => new URLSearchParams(params))),
        { initialValue: new URLSearchParams() },
    );

    /**
     * Kezdeti szűrőfeltételek a query paraméterekből, a szűrő komponens inicializálásához.
     * Csak egyszer számoljuk ki, a szűrő form ezután már maga kezeli az értékeit.
     */
    protected readonly filterInputDefaultValuesFromQuery = this.toFilterData(this.latestParams());

    constructor() {
        this.route.params.pipe(takeUntilDestroyed()).subscribe((params) => {
            if (params['id'] === undefined) {
                return;
            }
            const transactionIdFromParam = Number(params['id']);
            if (this.isValidTransactionId(transactionIdFromParam)) {
                this.modal.open(transactionIdFromParam);
            } else {
                console.error('Invalid transaction id in param: ' + transactionIdFromParam);
                void this.router.navigate(['/transactions'], { replaceUrl: true });
            }
        });

        // Modal bezárásakor kivesszük az id-t az URL-ből, a query paramétereket (szűrőket) megtartjuk
        this.modal.closed.pipe(takeUntilDestroyed()).subscribe(() => {
            if (this.route.snapshot.params['id'] !== undefined) {
                void this.router.navigate(['/transactions'], {
                    queryParamsHandling: 'preserve',
                    replaceUrl: true,
                });
            }
        });

        // Mentés/törlés után újratöltjük a listát, és jelezzük a szülő komponensnek is
        this.modal.changed.pipe(takeUntilDestroyed()).subscribe(() => {
            this.transactionListData.reload();
            this.transactionsChanged.emit();
        });

        // Ha az oldalszám túlmutat az utolsó oldalon (pl. kézzel írt URL, vagy törölték az utolsó
        // oldal utolsó elemét), az utolsó létező oldalra navigálunk
        effect(() => {
            if (this.transactionListData.status() !== 'resolved') {
                return;
            }
            const pageData = this.transactionListData.value().pageData;
            if (!pageData) {
                return;
            }
            const lastPageNumber = Math.max(0, Math.ceil(pageData.elementCount / pageData.elementPerPage) - 1);
            if (this.actualPageNumber() > lastPageNumber) {
                this.navigateToPage(lastPageNumber, true);
            }
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        // A szülő komponens ha üzen (új tranzakció felvételéről), akkor újratöltjük a listát
        if (changes['reloadTrigger'] && !changes['reloadTrigger'].firstChange) {
            this.transactionListData.reload();
        }
    }

    /**
     * Tranzakció id validálás
     */
    private isValidTransactionId(transactionId: number) {
        return Number.isInteger(transactionId) && transactionId > 0;
    }

    /**
     * Aktuális oldalszám. Érvénytelen (nem egész, negatív) érték esetén az első oldal
     */
    protected actualPageNumber = computed(() => {
        const pageNumber = Number(this.latestParams().get('pageNumber'));
        return Number.isInteger(pageNumber) && pageNumber > 0 ? pageNumber : 0;
    });

    /**
     * A backend felé küldött paraméterek: a query paraméterek, validált oldalszámmal
     */
    private readonly requestParams = computed(() => {
        const params = new URLSearchParams(this.latestParams());
        params.delete('pageNumber');

        const pageNumber = this.actualPageNumber();
        if (pageNumber > 0) {
            params.set('pageNumber', pageNumber.toString());
        }

        return params;
    });

    /**
     * Tranzakciós lista adatok. A query paraméterek változtatásakor magától betöltődik
     */
    protected transactionListData = resource({
        defaultValue: {
            transactions: [],
        } satisfies TransactionHistoryInterface,
        params: this.requestParams,
        loader: async ({ params }) => {
            const apiObserver = this.isHistoryMode()
                ? this.transactionService.getTransactionHistory(params)
                : this.transactionService.getLastTransactions();

            return firstValueFrom(apiObserver);
        },
    });

    /**
     * Lapozási adatok. Betöltés/hiba alatt az utolsó ismert értéket tartja meg, hogy a lapozó
     * ne tűnjön el (és ne vesszen el rajta a fókusz) minden lapváltáskor
     */
    protected readonly pageData = linkedSignal<
        TransactionHistoryInterface['pageData'] | undefined,
        TransactionHistoryInterface['pageData'] | undefined
    >({
        source: () =>
            this.transactionListData.hasValue() ? this.transactionListData.value().pageData : undefined,
        computation: (pageData, previous) => pageData ?? previous?.value,
    });

    /**
     * Szűrő form beküldésekor lefutó művelet: a route query paramétereinek frissítése.
     * A queryParams változásra reagálva újratölti a listát
     */
    protected onFilterSubmit(data: FilterData): void {
        void this.router.navigate([], {
            relativeTo: this.route,
            queryParams: this.toQueryParams(data),
        });
    }

    /**
     * Lapváltás lekezelése. Többi keresési feltételt megtartja
     */
    protected handlePageChange(event: PageEvent) {
        this.navigateToPage(event.pageIndex);
    }

    /**
     * Adott oldalra navigál, a többi query paramétert megtartva. Az első oldalnál kiveszi a
     * pageNumber-t az URL-ből
     */
    private navigateToPage(pageNumber: number, replaceUrl = false): void {
        void this.router.navigate([], {
            relativeTo: this.route,
            queryParams: { pageNumber: pageNumber > 0 ? pageNumber : null },
            queryParamsHandling: 'merge',
            replaceUrl,
        });
    }

    /**
     * Url paraméterek resetelése (filterform elemei is törlődnek + lapozó is visszaáll az első oldalra)
     */
    protected clearUrlParams(): void {
        void this.router.navigate([], {
            relativeTo: this.route,
            queryParams: {},
        });
    }

    /**
     * Diagramm megjelenik-e
     */
    protected isChartVisible = computed(() => {
        return this.isHistoryMode() && this.isThereEnoughElementToShowChart();
    });

    /**
     * Ennyi elemnél néz már ki valahogyan a diagram
     */
    protected isThereEnoughElementToShowChart = computed(() => {
        return (
            this.transactionListData.hasValue() &&
            this.transactionListData.value().transactions.length > 1
        );
    });

    /**
     * URLSearchParams-ból kiszedi a szűrőfeltételeket, amik be vannak állítva
     */
    private toFilterData(params: URLSearchParams): FilterData {
        const date = params.get('date');

        return {
            transactionId: params.get('transactionId') ? Number(params.get('transactionId')) : null,
            name: params.get('name') ?? '',
            date: date ? this.timeHelper.stringToDate(date) : null,
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
            params['date'] = this.timeHelper.formatDateToString(data.date);
        }

        if (data.transactionId && this.isValidTransactionId(data.transactionId)) {
            params['transactionId'] = data.transactionId.toString();
        }

        return params;
    }
}
