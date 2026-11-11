import { Component, computed, inject, input, output } from '@angular/core';
import {
    MonthlyStatInterface,
} from '../recurring-transactions/interfaces';
import { BaseChartDirective, provideCharts } from 'ng2-charts';
import { ArcElement, ChartData, ChartOptions, PieController, Tooltip } from 'chart.js';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { CurrencyCodesEnum, TransactionTypeEnum } from '@app/shared/enums';
import { CurrencyFormatPipe } from '@app/shared/pipes/currency-format-pipe';
import { CurrencyConversionService } from '@app/shared/services/currency-conversion-service';

/**
 * Kategorikus színek, fix sorrendben (színtévesztők számára is megkülönböztethető
 * szomszédos párok). 8-nál több szelet nem kap új színt, hanem az "Egyéb"-be kerül
 */
const SLICE_COLORS = [
    '#2a78d6',
    '#eb6834',
    '#1baf7a',
    '#eda100',
    '#e87ba4',
    '#008300',
    '#6250d6',
    '#e34948',
];
/** Az "Egyéb" szelet semleges szürke, hogy ne tűnjön egy újabb tételnek */
const OTHER_COLOR = '#898781';
/** Ennyi saját szelet fér el, a többi összevonva "Egyéb" lesz */
const MAX_SLICES = SLICE_COLORS.length - 1;
/** A tervezett rész ugyanaz a szín fél átlátszósággal (#rrggbb + 80 alfa) */
const PLANNED_ALPHA = '80';

/**
 * Egy szelet a diagramon és a mellette lévő listában
 */
interface Slice {
    /** Az ismétlődő tranzakció azonosítója, az "Egyéb" szeletnél nincs */
    id?: number;
    label: string;
    /** A már létrejött (lekönyvelt) összeg forintban */
    bookedAmount: number;
    /** A hónapban még tervezett összeg forintban */
    plannedAmount: number;
    /** A kettő összege forintban */
    hufAmount: number;
    color: string;
}

/**
 * Egy ív a diagramon: egy szelet lekönyvelt vagy tervezett része
 */
interface Arc {
    slice: Slice;
    planned: boolean;
    amount: number;
}

/**
 * Az adott havi ismétlődő kiadások megoszlása: tételenként a már létrejött és a
 * még tervezett összeg együtt, forintra átváltva
 */
@Component({
    selector: 'app-recurring-transaction-chart-component',
    templateUrl: './recurring-transaction-chart-component.html',
    styleUrl: './recurring-transaction-chart-component.scss',
    imports: [BaseChartDirective, TranslatePipe],
    providers: [
        CurrencyFormatPipe,
        // Így a bundle size türhető mértéken marad, mert akkor tölti csak be, ahol használva van a diagram
        provideCharts({ registerables: [PieController, ArcElement, Tooltip] }),
    ],
})
export class RecurringTransactionChartComponent {
    private readonly currencyFormatPipe = inject(CurrencyFormatPipe);
    private readonly translateService = inject(TranslateService);
    private readonly currencyConversionService = inject(CurrencyConversionService);

    public statData = input.required<MonthlyStatInterface[]>();

    public openRecurringEditModal = output<number>();

    /**
     * Szeletek: csak a kiadások (a bevétel és a kiadás nem egy egész részei),
     * összeg szerint csökkenő sorrendben. A legnagyobbak saját színt kapnak, a
     * többi "Egyéb" néven összevonva
     */
    protected slices = computed<Slice[]>(() => {
        const expenses = this.statData()
            .filter((stat) => stat.transactionType === TransactionTypeEnum.OUTCOME)
            .map((stat) => {
                const bookedAmount = Math.abs(
                    this.currencyConversionService.toHuf(stat.createdSum, stat.currencyCode),
                );
                const plannedAmount = Math.abs(
                    this.currencyConversionService.toHuf(stat.plannedSum, stat.currencyCode),
                );
                return {
                    id: stat.recurringTransactionId,
                    label: stat.name,
                    bookedAmount,
                    plannedAmount,
                    hufAmount: bookedAmount + plannedAmount,
                };
            })
            .filter((expense) => expense.hufAmount > 0)
            .sort((a, b) => b.hufAmount - a.hufAmount);

        const needsOther = expenses.length > SLICE_COLORS.length;
        const own = needsOther ? expenses.slice(0, MAX_SLICES) : expenses;

        // a szín az id sorrendjét követi, nem az összegét, így egy összeg változása
        // nem színezi át a többi szeletet
        const colorIndexById = new Map(
            [...own].sort((a, b) => a.id - b.id).map((expense, index) => [expense.id, index]),
        );
        const slices: Slice[] = own.map((expense) => ({
            ...expense,
            color: SLICE_COLORS[colorIndexById.get(expense.id) ?? 0] ?? OTHER_COLOR,
        }));

        if (needsOther) {
            const rest = expenses.slice(MAX_SLICES);
            const bookedAmount = rest.reduce((sum, expense) => sum + expense.bookedAmount, 0);
            const plannedAmount = rest.reduce((sum, expense) => sum + expense.plannedAmount, 0);
            slices.push({
                label: this.translateService.instant('recurring_transaction.chart.other') as string,
                bookedAmount,
                plannedAmount,
                hufAmount: bookedAmount + plannedAmount,
                color: OTHER_COLOR,
            });
        }
        return slices;
    });

    /**
     * Ívek: szeletenként előbb a lekönyvelt, utána a tervezett rész, egymás mellett.
     * A nulla összegű részek kimaradnak, hogy ne rajzolódjon üres ív a résekkel
     */
    private arcs = computed<Arc[]>(() =>
        this.slices().flatMap((slice) =>
            [
                { slice, planned: false, amount: slice.bookedAmount },
                { slice, planned: true, amount: slice.plannedAmount },
            ].filter((arc) => arc.amount > 0),
        ),
    );

    /**
     * Az ív színe: a tervezett rész a szelet színe fél átlátszósággal
     */
    private arcColor(arc: Arc): string {
        return arc.planned ? arc.slice.color + PLANNED_ALPHA : arc.slice.color;
    }

    /**
     * A szeletek összege forintban
     */
    protected total = computed(() =>
        this.slices().reduce((sum, slice) => sum + slice.hufAmount, 0),
    );

    protected formatHuf(amount: number): string {
        return this.currencyFormatPipe.transform(amount, CurrencyCodesEnum.huf);
    }

    /**
     * A szelet aránya az egészhez képest, egész százalékra kerekítve
     */
    protected sharePercent(amount: number): number {
        const total = this.total();
        return total === 0 ? 0 : Math.round((amount / total) * 100);
    }

    /**
     * Adatok átalakítása a chartjs számára
     */
    protected chartData = computed<ChartData<'pie'>>(() => {
        const arcs = this.arcs();
        const colors = arcs.map((arc) => this.arcColor(arc));
        return {
            labels: arcs.map((arc) => arc.slice.label),
            datasets: [
                {
                    data: arcs.map((arc) => arc.amount),
                    backgroundColor: colors,
                    hoverBackgroundColor: colors,
                    // a szeletek közötti 2px-es rés a háttér színével
                    borderColor: '#ffffff',
                    borderWidth: 2,
                    hoverOffset: 4,
                },
            ],
        };
    });

    protected chartOptions: ChartOptions<'pie'> = {
        responsive: true,
        maintainAspectRatio: false,
        onClick: (_event, elements) => {
            const index = elements[0]?.index;
            // az "Egyéb" szeletnek nincs azonosítója, az nem nyitható fel
            const id = index === undefined ? undefined : this.arcs()[index]?.slice.id;
            if (id !== undefined) {
                this.openRecurringEditModal.emit(id);
            }
        },
        plugins: {
            // a jelmagyarázat HTML-ben van a diagram mellett, névvel és összeggel
            legend: { display: false },
            tooltip: {
                callbacks: {
                    label: (context) => {
                        const arc = this.arcs()[context.dataIndex];
                        const kind = this.translateService.instant(
                            arc?.planned
                                ? 'recurring_transaction.chart.planned'
                                : 'recurring_transaction.chart.booked',
                        ) as string;
                        return `${kind}: ${this.formatHuf(context.parsed)} (${this.sharePercent(context.parsed)}%)`;
                    },
                },
            },
        },
    };
}
