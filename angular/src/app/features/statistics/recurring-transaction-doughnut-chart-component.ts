import { Component, computed, inject, input, output } from '@angular/core';
import {
    MonthlyStatInterface,
} from '../recurring-transactions/interfaces';
import { BaseChartDirective, provideCharts } from 'ng2-charts';
import { ArcElement, ChartData, ChartOptions, DoughnutController, Tooltip } from 'chart.js';
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

/**
 * Egy szelet a diagramon és a mellette lévő listában
 */
interface Slice {
    label: string;
    hufAmount: number;
    color: string;
}

/**
 * Az adott havi ismétlődő kiadások megoszlása: tételenként a már létrejött és a
 * még tervezett összeg együtt, forintra átváltva
 */
@Component({
    selector: 'app-recurring-transaction-doughnut-chart-component',
    templateUrl: './recurring-transaction-doughnut-chart-component.html',
    styleUrl: './recurring-transaction-doughnut-chart-component.scss',
    imports: [BaseChartDirective, TranslatePipe],
    providers: [
        CurrencyFormatPipe,
        // Így a bundle size türhető mértéken marad, mert akkor tölti csak be, ahol használva van a diagram
        provideCharts({ registerables: [DoughnutController, ArcElement, Tooltip] }),
    ],
})
export class RecurringTransactionDoughnutComponent {
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
            .map((stat) => ({
                id: stat.recurringTransactionId,
                label: stat.name,
                hufAmount: Math.abs(
                    this.currencyConversionService.toHuf(
                        stat.createdSum + stat.plannedSum,
                        stat.currencyCode,
                    ),
                ),
            }))
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
            label: expense.label,
            hufAmount: expense.hufAmount,
            color: SLICE_COLORS[colorIndexById.get(expense.id) ?? 0] ?? OTHER_COLOR,
        }));

        if (needsOther) {
            const otherAmount = expenses
                .slice(MAX_SLICES)
                .reduce((sum, expense) => sum + expense.hufAmount, 0);
            slices.push({
                label: this.translateService.instant('recurring_transaction.chart.other') as string,
                hufAmount: otherAmount,
                color: OTHER_COLOR,
            });
        }
        return slices;
    });

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
    protected chartData = computed<ChartData<'doughnut'>>(() => {
        const slices = this.slices();
        return {
            labels: slices.map((slice) => slice.label),
            datasets: [
                {
                    data: slices.map((slice) => slice.hufAmount),
                    backgroundColor: slices.map((slice) => slice.color),
                    hoverBackgroundColor: slices.map((slice) => slice.color),
                    // a szeletek közötti 2px-es rés a háttér színével
                    borderColor: '#ffffff',
                    borderWidth: 2,
                    hoverOffset: 4,
                },
            ],
        };
    });

    /**
     * Meghatározza a sorszám alapján a recurringTransaction adatot.
     * Az utolsó elem az other, ezért ott már undefined-el tér vissza
     * 
     * @param index 
     * @returns 
     */
    private recurringTransactionAt(index: number | undefined): MonthlyStatInterface | undefined {
        if (!index || index >= MAX_SLICES) {
            return undefined
        }
        return index === undefined ? undefined : this.statData()[index];
    }

    protected chartOptions: ChartOptions<'doughnut'> = {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '65%',
        onClick: (_event, elements) => {
            const recurringTransaction = this.recurringTransactionAt(elements[0]?.index);
            if (recurringTransaction) {
                this.openRecurringEditModal.emit(recurringTransaction.recurringTransactionId);
            }
        },
        plugins: {
            // a jelmagyarázat HTML-ben van a diagram mellett, névvel és összeggel
            legend: { display: false },
            tooltip: {
                callbacks: {
                    label: (context) =>
                        `${this.formatHuf(context.parsed)} (${this.sharePercent(context.parsed)}%)`,
                },
            },
        },
    };
}
