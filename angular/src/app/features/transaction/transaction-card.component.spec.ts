import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import TransactionCardComponent from './transaction-card-component';
import { DecimalPipe } from '@angular/common';
import { TransactionTypeEnum } from './transaction-type-enum';

describe('TransactionCardComponent (Vitest)', () => {
    let fixture: ComponentFixture<TransactionCardComponent>;
    let component: TransactionCardComponent;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [TransactionCardComponent, DecimalPipe],
        }).compileComponents();

        fixture = TestBed.createComponent(TransactionCardComponent);
        component = fixture.componentInstance;
    });

    it('should render income transaction correctly', () => {
        // GIVEN
        component.transaction = {
            id: 25,
            name: 'Fizetés',
            priceSum: 500000,
            transactionType: TransactionTypeEnum.INCOME,
            transactionDate: '2024-01-10',
            isComplexTransaction: false,
            transactionDetails: [
                {
                    name: 'sum',
                    price: 500000,
                    categories: [],
                    weight: null,
                    unitPrice: null,
                    isComplexPriceMode: false,
                },
            ],
        };

        // WHEN
        fixture.detectChanges();

        // THEN
        const priceEl = fixture.nativeElement.querySelector('.transaction-price');
        expect(priceEl.classList.contains('income')).toBe(true);
        expect(priceEl.classList.contains('outcome')).toBe(false);
        expect(priceEl.textContent.trim()).toBe('500,000');

        const name = fixture.nativeElement.querySelector('.transaction-name');
        expect(name.textContent.trim()).toBe('Fizetés');

        const date = fixture.nativeElement.querySelector('.transaction-date');
        expect(date.textContent.trim()).toBe('2024-01-10');

        const categoriesEl = fixture.nativeElement.querySelector('.transaction-categories');
        expect(categoriesEl).toBe(null);

        const badge = fixture.nativeElement.querySelector('.transaction-icon-badge');
        expect(badge.classList.contains('income')).toBe(true);

        const badgeClass = fixture.nativeElement.querySelector('.transaction-icon-badge i');
        expect(badgeClass.classList.contains('bi-arrow-up-short')).toBe(true);
    });

    it('should render outcome transaction correctly', () => {
        // GIVEN
        component.transaction = {
            id: 26,
            isComplexTransaction: true,
            name: 'Bevásárlás',
            priceSum: -1000,
            transactionType: TransactionTypeEnum.OUTCOME,
            transactionDate: '2024-01-11',
            transactionDetails: [
                {
                    name: 'kakaós csiga',
                    price: -500,
                    categories: ['élelmiszer', 'nasi'],
                    weight: null,
                    unitPrice: null,
                    isComplexPriceMode: false,
                },
                {
                    name: 'kenyér',
                    price: -500,
                    categories: ['élelmiszer'],
                    weight: 0.5,
                    unitPrice: 1000,
                    isComplexPriceMode: true,
                },
            ],
        };

        // WHEN
        fixture.detectChanges();

        // THEN
        const priceEl = fixture.nativeElement.querySelector('.transaction-price');
        expect(priceEl.classList.contains('outcome')).toBe(true);
        expect(priceEl.classList.contains('income')).toBe(false);
        expect(priceEl.textContent.trim()).toBe('-1,000');

        const name = fixture.nativeElement.querySelector('.transaction-name');
        expect(name.textContent.trim()).toBe('Bevásárlás');

        const date = fixture.nativeElement.querySelector('.transaction-date');
        expect(date.textContent.trim()).toBe('2024-01-11');

        const categoriesEl = fixture.nativeElement.querySelector('.transaction-categories');
        expect(categoriesEl.textContent.trim()).toBe('élelmiszer, nasi');

        const badge = fixture.nativeElement.querySelector('.transaction-icon-badge');
        expect(badge.classList.contains('outcome')).toBe(true);

        const badgeClass = fixture.nativeElement.querySelector('.transaction-icon-badge i');
        expect(badgeClass.classList.contains('bi-arrow-down-short')).toBe(true);
    });

});
