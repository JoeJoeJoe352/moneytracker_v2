import { Component, computed, inject, input, output } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { TransactionListElementData } from '../transaction/interfaces';
import { CurrencyCodesEnum, TransactionTypeEnum } from '@app/shared/enums';
import { BaseChartDirective, provideCharts } from 'ng2-charts';
import {
    BarController,
    BarElement,
    CategoryScale,
    ChartData,
    ChartOptions,
    LinearScale,
    Tooltip,
} from 'chart.js';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { CurrencyFormatPipe } from '@app/shared/pipes/currency-format-pipe';
import { getLocaleForLang } from '@app/shared/utils/language-util';
import { CurrencyConversionService } from '@app/shared/services/currency-conversion-service';
import { indexOfMax } from '@app/shared/utils/transaction-util';
import { TimeHelper } from '@app/shared/services/time-helper';

@Component({
    selector: 'app-transaction-bar-chart-component',
    templateUrl: './transaction-bar-chart-component.html',
    styleUrl: './transaction-bar-chart-component.scss',
    imports: [BaseChartDirective, TranslatePipe],
    providers: [
        CurrencyFormatPipe,
        // Így a bundle size türhető mértéken marad, mert akkor tölti csak be, ahol használva van a diagram
        provideCharts({
            registerables: [BarController, BarElement, CategoryScale, LinearScale, Tooltip],
        }),
    ],
})
export class TransactionBarChartComponent {
    private readonly currencyFormatPipe = inject(CurrencyFormatPipe);
    private readonly translateService = inject(TranslateService);
    private readonly currencyConversionService = inject(CurrencyConversionService);
    private readonly document = inject(DOCUMENT);
    private readonly timeHelper = inject(TimeHelper);

    /**
     * Tranzakciós adatok a chartnak
     */
    public transactionList = input.required<TransactionListElementData[]>();

    /**
     * Akkor emitál, amikor a user rákattint egy oszlopra (a tranzakció szerkesztéséhez)
     */
    public editTransaction = output<{ transactionId: number }>();

    // a canvas nem érti a CSS változókat, ezért itt kiolvassuk a tényleges színeket
    private readonly rootStyle = getComputedStyle(this.document.documentElement);
    private readonly expenseColor = this.rootStyle.getPropertyValue('--moneytracker-red').trim();
    private readonly incomeColor = this.rootStyle.getPropertyValue('--moneytracker-green').trim();

    /**
     * Az oszlopok értékei forintban. Minden érték pozitív
     */
    private hufPrices = computed(() =>
        this.transactionList().map((transaction) =>
            Math.abs(
                this.currencyConversionService.toHuf(
                    transaction.priceSum,
                    transaction.wallet.currencyCode,
                ),
            ),
        ),
    );

    /**
     * Kiadás-e a tranzakció
     */
    private isExpense(transaction: TransactionListElementData): boolean {
        return transaction.transactionType === TransactionTypeEnum.OUTCOME;
    }

    /**
     * A diagram szöveges összefoglalója a felolvasóknak (a canvas tartalmát nem tudják felolvasni)
     */
    protected summary = computed(() => {
        const data = this.transactionList();
        if (data.length === 0) {
            return { key: 'transaction_page.chart.summary.empty', params: {} };
        }

        const hufPrices = this.hufPrices();
        const largestIndex = indexOfMax(hufPrices);
        const largest = data[largestIndex];
        const largestPrice = hufPrices[largestIndex];
        if (largest === undefined || largestPrice === undefined) {
            return { key: 'transaction_page.chart.summary.empty', params: {} };
        }

        const expenseCount = data.filter((transaction) => this.isExpense(transaction)).length;

        return {
            key: 'transaction_page.chart.summary',
            params: {
                count: data.length,
                incomeCount: data.length - expenseCount,
                expenseCount: expenseCount,
                name: largest.name,
                amount: this.currencyFormatPipe.transform(largestPrice, CurrencyCodesEnum.huf),
            },
        };
    });

    /**
     * A chart adott indexű oszlopához tartozó tranzakció
     */
    private transactionAt(index: number | undefined): TransactionListElementData | undefined {
        return index === undefined ? undefined : this.transactionList()[index];
    }

    /**
     * Adatok átalakítása a chartjs számára
     */
    protected chartData = computed<ChartData<'bar'>>(() => {
        // oszloponként a tranzakció típusának megfelelő szín
        const colors = this.transactionList().map((transaction) =>
            this.isExpense(transaction) ? this.expenseColor : this.incomeColor,
        );
        // a kiadások a tengely alá kerülnek, így nem csak a szín különbözteti meg a típusokat
        const signedHufPrices = this.hufPrices().map((price, index) => {
            const transaction = this.transactionAt(index);
            return transaction && this.isExpense(transaction) ? -price : price;
        });

        return {
            labels: this.transactionList().map(
                (transactionData) => transactionData.name,
            ),
            datasets: [
                {
                    data: signedHufPrices,
                    backgroundColor: colors,
                    hoverBackgroundColor: colors,
                    borderRadius: 4,
                },
            ],
        };
    });

    /**
     * Chart beállítások. Azért computed, mert a nyelv változhat és akkor újra kell generálni a beállításokat
     */
    protected chartOptions = computed<ChartOptions<'bar'>>(() => {
        const lang = this.translateService.currentLang() ?? this.translateService.getFallbackLang();
        const dateFormat = new Intl.DateTimeFormat(getLocaleForLang(lang), {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
        });

        return {
            responsive: true,
            // enélkül a Chart.js 2:1 képarányt tart, és nem veszi figyelembe a konténer magasságát
            maintainAspectRatio: false,
            // az oszlop teljes magasságában lehet rá mutatni/kattintani, nem csak a (néha nagyon kicsi) oszlopon
            interaction: { mode: 'index', intersect: false },

            onClick: (_event, elements) => {
                const transaction = this.transactionAt(elements[0]?.index);
                if (transaction) {
                    this.editTransaction.emit({ transactionId: transaction.id });
                }
            },
            onHover: (event, elements) => {
                const canvas = event.native?.target as HTMLElement | undefined;
                if (canvas) {
                    canvas.style.cursor = elements.length > 0 ? 'pointer' : 'default';
                }
            },

            scales: {
                y: {
                    ticks: {
                        callback: (value) =>
                            this.currencyFormatPipe.transform(Number(value), CurrencyCodesEnum.huf),
                    },
                },
            },
            plugins: {
                legend: {
                    display: false,
                },
                tooltip: {
                    callbacks: {
                        // az azonos nevű tranzakciókat a dátum különbözteti meg
                        title: (contexts) => {
                            const transaction = this.transactionAt(contexts[0]?.dataIndex);
                            if (!transaction) {
                                return '';
                            }
                            const date = this.timeHelper.stringToDate(transaction.transactionDate)
                            return `${transaction.name} · ${dateFormat.format(date)}`;
                        },
                        label: (context) => {
                            const transaction = this.transactionAt(context.dataIndex);
                            const hufPrice = this.currencyFormatPipe.transform(
                                Math.abs(context.parsed.y ?? 0),
                                CurrencyCodesEnum.huf,
                            );
                            if (!transaction) {
                                return `${hufPrice}`;
                            }
                            const type = this.translateService.instant(
                                this.isExpense(transaction)
                                    ? 'transaction.expense'
                                    : 'transaction.income',
                            ) as string;
                            const currencyCode = transaction.wallet.currencyCode;

                            if (currencyCode === CurrencyCodesEnum.huf) {
                                return `${type}: ${hufPrice}`;
                            }
                            const price = this.currencyFormatPipe.transform(
                                Math.abs(transaction.priceSum),
                                currencyCode,
                            );
                            return `${type}: ${price} (≈${hufPrice})`;
                        },
                    },
                },
            },
        };
    });
}
