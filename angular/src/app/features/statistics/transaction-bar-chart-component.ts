import { Component, computed, inject, input, output, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { TransactionListElementData } from '../transaction/interfaces';
import { CurrencyCodesEnum, SupportedLangEnum, TransactionTypeEnum } from '@app/shared/enums';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { CurrencyFormatPipe } from '@app/shared/pipes/currency-format-pipe';
import { LANGUAGE_TO_LOCALE } from '@app/shared/utils/language-util';

// TODO ezeket majd éles adatokkal számolni
const CONVERTING_VALUE_FROM_EUR = 365;
const CONVERTING_VALUE_FROM_USD = 320;

@Component({
    selector: 'app-transaction-bar-chart-component',
    templateUrl: './transaction-bar-chart-component.html',
    styleUrl: './transaction-bar-chart-component.scss',
    imports: [BaseChartDirective, MatButtonToggleGroup, MatButtonToggle, TranslatePipe],
    providers: [CurrencyFormatPipe],
})
export class TransactionBarChartComponent {
    private readonly currencyFormatPipe = inject(CurrencyFormatPipe);
    private readonly translateService = inject(TranslateService);
    private readonly document = inject(DOCUMENT);

    /**
     * Tranzakciós adatok a chartnak
     */
    public transactionList = input.required<TransactionListElementData[]>();

    /**
     * Akkor emitál, amikor a user rákattint egy oszlopra (a tranzakció szerkesztéséhez)
     */
    public editTransaction = output<{ transactionId: number }>();

    /**
     * A kiadás oldali diagrammot mutatjuk-e a usernek
     */
    protected isExpenseMode = signal(true);

    // a canvas nem érti a CSS változókat, ezért egyszer kiolvassuk a tényleges színeket
    private readonly rootStyle = getComputedStyle(this.document.documentElement);
    private readonly expenseColor = this.rootStyle.getPropertyValue('--moneytracker-red').trim();
    private readonly incomeColor = this.rootStyle.getPropertyValue('--moneytracker-green').trim();

    /**
     * Tranzakciós adatok, a tranzakció típus által szűrve
     */
    private transactionDataFilteredByType = computed(() => {
        const values = this.transactionList().filter((transaction) => {
            return this.isExpenseMode()
                ? transaction.transactionType === TransactionTypeEnum.OUTCOME
                : transaction.transactionType === TransactionTypeEnum.INCOME;
        });
        values.reverse();
        return values;
    });

    /**
     * Valuták közti átváltás
     */
    private getPriceInHuf(transaction: TransactionListElementData) {
        switch (transaction.wallet.currencyCode) {
            case CurrencyCodesEnum.huf:
                return transaction.priceSum;
            case CurrencyCodesEnum.eur:
                return transaction.priceSum * CONVERTING_VALUE_FROM_EUR;
            case CurrencyCodesEnum.usd:
                return transaction.priceSum * CONVERTING_VALUE_FROM_USD;
        }
    }

    /**
     * Az oszlopok értékei forintban (a diagram, az összesítés és az összefoglaló is ezt használja)
     */
    private hufPrices = computed(() =>
        this.transactionDataFilteredByType().map((transaction) => Math.abs(this.getPriceInHuf(transaction))),
    );

    /**
     * Nincs megjeleníthető tranzakció a kiválasztott típusból
     */
    protected isEmpty = computed(() => this.transactionDataFilteredByType().length === 0);

    /**
     * A megjelenített tranzakciók forintos összege, formázva
     */
    protected formattedTotal = computed(() => {
        const total = this.hufPrices().reduce((sum, price) => sum + price, 0);
        return this.currencyFormatPipe.transform(total, CurrencyCodesEnum.huf);
    });

    /**
     * A megjelenített tranzakció típus fordítási kulcsa
     */
    protected typeLabelKey = computed(() =>
        this.isExpenseMode() ? 'transaction.expenses' : 'transaction.incomes',
    );

    /**
     * A diagram szöveges összefoglalója a felolvasóknak (a canvas tartalmát nem tudják felolvasni)
     */
    protected summary = computed(() => {
        const data = this.transactionDataFilteredByType();
        if (data.length === 0) {
            return { key: 'transaction_page.chart.summary.empty', params: {} };
        }

        const hufPrices = this.hufPrices();
        const largestIndex = hufPrices.indexOf(Math.max(...hufPrices));

        return {
            key: 'transaction_page.chart.summary',
            params: {
                count: data.length,
                total: this.formattedTotal(),
                name: data[largestIndex].name,
                amount: this.currencyFormatPipe.transform(hufPrices[largestIndex], CurrencyCodesEnum.huf),
            },
        };
    });

    /**
     * Adatok átalakítása a chartjs számára
     */
    protected chartData = computed<ChartData<'bar'>>(() => {
        const color = this.isExpenseMode() ? this.expenseColor : this.incomeColor;

        return {
            labels: this.transactionDataFilteredByType().map((transactionData) => transactionData.name),
            datasets: [
                {
                    data: this.hufPrices(),
                    backgroundColor: color,
                    hoverBackgroundColor: color,
                    borderRadius: 4,
                },
            ],
        };
    });

    /**
     * Chart beállítások. Computed, mert a nyelv olvasása miatt nyelvváltáskor új objektum jön létre,
     * és a chart újrarajzolja a tengely feliratait az új formátummal
     */
    protected options = computed<ChartOptions<'bar'>>(() => {
        const lang = this.translateService.currentLang() ?? this.translateService.getFallbackLang();
        const dateFormat = new Intl.DateTimeFormat(
            LANGUAGE_TO_LOCALE[lang ?? ''] ?? LANGUAGE_TO_LOCALE[SupportedLangEnum.en],
            { year: 'numeric', month: 'short', day: 'numeric' },
        );

        return {
            responsive: true,
            // enélkül a Chart.js 2:1 képarányt tart, és nem veszi figyelembe a konténer magasságát
            maintainAspectRatio: false,
            // az oszlop teljes magasságában lehet rá mutatni/kattintani, nem csak a (néha nagyon kicsi) oszlopon
            interaction: { mode: 'index', intersect: false },

            onClick: (_event, elements) => {
                if (elements.length > 0) {
                    const transaction = this.transactionDataFilteredByType()[elements[0].index];
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
                        // az azonos nevű tranzakciókat (pl. több "Lidl") a dátum különbözteti meg
                        title: (contexts) => {
                            const transaction = this.transactionDataFilteredByType()[contexts[0].dataIndex];
                            const [year, month, day] = transaction.transactionDate.split('-').map(Number);
                            return `${transaction.name} · ${dateFormat.format(new Date(year, month - 1, day))}`;
                        },
                        label: (context) => {
                            const transaction = this.transactionDataFilteredByType()[context.dataIndex];
                            const hufPrice = this.currencyFormatPipe.transform(
                                context.parsed.y ?? 0,
                                CurrencyCodesEnum.huf,
                            );
                            const currencyCode = transaction.wallet.currencyCode;

                            if (currencyCode === CurrencyCodesEnum.huf) {
                                return `${hufPrice}`;
                            }
                            const price = this.currencyFormatPipe.transform(
                                Math.abs(transaction.priceSum),
                                currencyCode,
                            );
                            return `${price} (≈${hufPrice})`;
                        },
                    },
                },
            },
        };
    });
}
