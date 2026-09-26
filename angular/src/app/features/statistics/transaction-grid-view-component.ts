import { Component, computed, inject, input, signal } from '@angular/core';
import { TransactionListElementData } from '../transaction/interfaces';
import { CurrencyCodesEnum, TransactionTypeEnum } from '@app/shared/enums';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { CurrencyFormatPipe } from '@app/shared/pipes/currency-format-pipe';

// TODO ezeket majd éles adatokkal számolni
const CONVERTING_VALUE_FROM_EUR = 365;
const CONVERTING_VALUE_FROM_USD = 320;

@Component({
    selector: 'app-transaction-grid-view-component',
    templateUrl: './transaction-grid-view-component.html',
    styleUrl: './transaction-grid-view-component.scss',
    imports: [BaseChartDirective, MatSlideToggle, TranslatePipe],
    providers: [CurrencyFormatPipe],
})
export class TransactionGridViewComponent {
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
