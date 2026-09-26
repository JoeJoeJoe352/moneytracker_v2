import { Component, computed, input, Signal } from '@angular/core';
import { TransactionListElementData } from '../transaction/interfaces';
import { CurrencyCodesEnum, TransactionTypeEnum } from '@app/shared/enums';
import { CurrencyFormatPipe } from '@app/shared/pipes/currency-format-pipe';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';

const CONVERTING_VALUE_FROM_EUR = 365;
const CONVERTING_VALUE_FROM_USD = 320;

interface GridViewDataInterface {
    transactionId: number;
    normalisedPrice: number;
    originalPrice: number;
    currencySymbol: CurrencyCodesEnum;
}

@Component({
    selector: 'app-transaction-grid-view-component',
    templateUrl: './transaction-grid-view-component.html',
    styleUrl: './transaction-grid-view-component.scss',
    imports: [CurrencyFormatPipe, BaseChartDirective],
})
export class TransactionGridViewComponent {
    public transactionList = input.required<TransactionListElementData[]>();

    private valuesWithoutIncomes = computed(() => {
        return this.transactionList().filter(
            (transaction) => transaction.transactionType === TransactionTypeEnum.OUTCOME,
        );
    });

    private minPrice = computed(() => {
        return this.valuesWithoutIncomes().reduce((prev, current) => {
            const hufPrice = this.getPriceInHuf(current);
            return prev && prev < hufPrice ? prev : hufPrice;
        }, Number.MAX_SAFE_INTEGER);
    });

    private maxPrice = computed(() => {
        return this.valuesWithoutIncomes().reduce((prev, current) => {
            const hufPrice = this.getPriceInHuf(current);
            return prev && prev > hufPrice ? prev : hufPrice;
        }, 0);
    });

    protected dataForDiagram = computed<GridViewDataInterface[]>(() => {
        if (this.valuesWithoutIncomes().length === 0) {
            return [];
        }

        return this.valuesWithoutIncomes().map((transaction) => {
            const hufPrice = this.getPriceInHuf(transaction);

            const normalizedPrice = Math.abs(this.normalizePrice(hufPrice));
            const normalizedPriceToPositiveNumber = 1 - normalizedPrice;
            const normalizedPriceWithTreshold =
                normalizedPriceToPositiveNumber > 0.08 ? normalizedPriceToPositiveNumber : 0.08;
            return {
                transactionId: transaction.id,
                currencySymbol: transaction.wallet.currencyCode,
                originalPrice: transaction.priceSum,
                normalisedPrice: normalizedPriceWithTreshold,
            };
        });
    });

    private normalizePrice(price: number) {
        return (price - this.minPrice()) / (this.maxPrice() - this.minPrice());
    }

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

    protected chartData = computed(() => {
        const expenseValues = this.transactionList().filter(
            (transaction) => transaction.transactionType === TransactionTypeEnum.OUTCOME,
        );
        expenseValues.reverse()
        /*
        const incomeValues = this.transactionList().filter(
            (transaction) => transaction.transactionType === TransactionTypeEnum.INCOME,
        );
        */
        return {
            labels: expenseValues.map((transactionData) => transactionData.name),
            datasets: [
                {
                    label: 'Kiadás',
                    data: expenseValues.map((transactionData) =>
                        Math.abs(this.getPriceInHuf(transactionData)),
                    ),
                    borderWidth: 1,
                },
            ],
        };
    }) as Signal<ChartData<'bar'>>;

    protected options = {
        scales: {},
    } as ChartOptions<'bar'>;
}
