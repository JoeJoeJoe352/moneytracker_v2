import { Component, computed, inject, input, signal } from '@angular/core';
import { TransactionListElementData } from '../transaction/interfaces';
import { CurrencyCodesEnum, TransactionTypeEnum } from '@app/shared/enums';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { CurrencyFormatPipe } from '@app/shared/pipes/currency-format-pipe';

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

    /**
     * Tranzakciós adatok a chartnak
     */
    public transactionList = input.required<TransactionListElementData[]>();

    /**
     * A kiadás oldali diagrammot mutatjuk-e a usernek
     */
    protected isExpenseMode = signal(true);

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

        const hufPrices = data.map((transaction) => Math.abs(this.getPriceInHuf(transaction)));
        const largestIndex = hufPrices.indexOf(Math.max(...hufPrices));
        const total = hufPrices.reduce((sum, price) => sum + price, 0);

        return {
            key: 'transaction_page.chart.summary',
            params: {
                count: data.length,
                total: this.currencyFormatPipe.transform(total, CurrencyCodesEnum.huf),
                name: data[largestIndex].name,
                amount: this.currencyFormatPipe.transform(hufPrices[largestIndex], CurrencyCodesEnum.huf),
            },
        };
    });

    /**
     * Adatok átalakítása a chartjs számára
     */
    protected chartData = computed<ChartData<'bar'>>(() => {
        const data = this.transactionDataFilteredByType();

        return {
            labels: data.map((transactionData) => transactionData.name),
            datasets: [
                {
                    data: data.map((transactionData) =>
                        Math.abs(this.getPriceInHuf(transactionData)),
                    ),
                    borderWidth: 1,
                },
            ],
        };
    });

    /**
     * Chart beállítások. Computed, mert a nyelv olvasása miatt nyelvváltáskor új objektum jön létre,
     * és a chart újrarajzolja a tengely feliratait az új formátummal
     */
    protected options = computed<ChartOptions<'bar'>>(() => {
        this.translateService.currentLang();
        return {
            responsive: true,
            // enélkül a Chart.js 2:1 képarányt tart, és nem veszi figyelembe a konténer magasságát
            maintainAspectRatio: false,

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
