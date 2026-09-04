import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import TransactionListComponent from './transaction-list-component';
import TransactionCardComponent from './transaction-card-component';
import { TransactionTypeEnum } from '../transaction/transaction-type-enum';
import { TransactionListElementData } from './interfaces';
import { CurrencyCodesEnum, WalletTypesEnum } from '../../shared/enums';

const sampleWallet = {
    id: 1,
    name: 'Napi költés',
    currencyCode: CurrencyCodesEnum.huf,
    type: WalletTypesEnum.default,
};

describe('TransactionList + TransactionCard integration (Vitest)', () => {
    let fixture: ComponentFixture<TransactionListComponent>;
    let component: TransactionListComponent;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [TransactionListComponent],
            providers: [provideTranslateService()],
        }).compileComponents();

        TestBed.inject(TranslateService).use('hu');

        fixture = TestBed.createComponent(TransactionListComponent);
        component = fixture.componentInstance;
    });

    it('should render each transaction through the real card component with correct data', () => {
        // GIVEN
        component.isTransactionListLoading = false;
        component.title = null;
        component.transactions = [
            {
                id: 1,
                name: 'Fizetés',
                priceSum: 500000,
                transactionType: TransactionTypeEnum.INCOME,
                transactionDate: '2024-01-10',
                isComplexTransaction: false,
                transactionDetails: [],
                wallet: sampleWallet,
            },
            {
                id: 2,
                name: 'Bevásárlás',
                priceSum: -1000,
                transactionType: TransactionTypeEnum.OUTCOME,
                transactionDate: '2024-01-11',
                isComplexTransaction: true,
                transactionDetails: [
                    {
                        name: 'kenyér',
                        price: -1000,
                        categories: ['élelmiszer', 'nasi'],
                        weight: null,
                        unitPrice: null,
                        isComplexPriceMode: false,
                    },
                ],
                wallet: sampleWallet,
            },
        ] as TransactionListElementData[];

        // WHEN
        fixture.detectChanges();

        // THEN
        const cards = fixture.nativeElement.querySelectorAll('app-transaction-card');
        expect(cards.length).toBe(2);

        const firstPrice = cards[0].querySelector('.transaction-price');
        expect(firstPrice.classList.contains('income')).toBe(true);
        expect(firstPrice.textContent.replace(/\s+/g, ' ').trim()).toBe('500 000 Ft');
        expect(cards[0].querySelector('.transaction-name').textContent.trim()).toBe('Fizetés');
        expect(cards[0].querySelector('.transaction-categories')).toBe(null);

        const secondPrice = cards[1].querySelector('.transaction-price');
        expect(secondPrice.classList.contains('outcome')).toBe(true);
        expect(secondPrice.textContent.replace(/\s+/g, ' ').trim()).toBe('-1 000 Ft');
        expect(cards[1].querySelector('.transaction-name').textContent.trim()).toBe('Bevásárlás');
        expect(cards[1].querySelector('.transaction-categories').textContent.trim()).toBe(
            'élelmiszer, nasi',
        );
    });

    it('should emit editTransaction with the id of the clicked card, not just the first one', () => {
        // GIVEN
        component.isTransactionListLoading = false;
        component.transactions = [
            {
                id: 10,
                name: 'tranzakció1',
                priceSum: 100,
                transactionType: TransactionTypeEnum.INCOME,
                transactionDate: '2024-01-01',
                isComplexTransaction: false,
                transactionDetails: [],
                wallet: sampleWallet,
            },
            {
                id: 20,
                name: 'tranzakció2',
                priceSum: -200,
                transactionType: TransactionTypeEnum.OUTCOME,
                transactionDate: '2024-01-02',
                isComplexTransaction: false,
                transactionDetails: [],
                wallet: sampleWallet,
            },
        ];

        let emitted: { transactionId: number } | undefined;
        component.editTransaction.subscribe((event) => (emitted = event));

        fixture.detectChanges();

        // WHEN
        const cards = fixture.nativeElement.querySelectorAll('app-transaction-card');
        cards[1].click();

        // THEN
        expect(emitted).toEqual({ transactionId: 20 });
    });

    it('should instantiate a real TransactionCardComponent instance for each transaction', () => {
        // GIVEN
        component.isTransactionListLoading = false;
        component.transactions = [
            {
                id: 1,
                name: 'tranzakció1',
                priceSum: 100,
                transactionType: TransactionTypeEnum.INCOME,
                transactionDate: '2024-01-01',
                isComplexTransaction: false,
                transactionDetails: [],
                wallet: sampleWallet,
            },
        ];

        // WHEN
        fixture.detectChanges();

        // THEN
        const cardDebugElement = fixture.debugElement.query(By.directive(TransactionCardComponent));
        const cardComponent = cardDebugElement.componentInstance as TransactionCardComponent;

        expect(cardComponent).toBeInstanceOf(TransactionCardComponent);
        expect(cardComponent.transaction.name).toBe('tranzakció1');
    });
});
