import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import TransactionListComponent from './transaction-list-component';
import TransactionCardComponent from './transaction-card-component';
import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';

describe('TransactionListComponent (Vitest)', () => {
    let fixture: ComponentFixture<TransactionListComponent>;
    let component: TransactionListComponent;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [TransactionListComponent, TransactionCardComponent, TranslatePipe],
            providers: [provideTranslateService()],
        }).compileComponents();

        fixture = TestBed.createComponent(TransactionListComponent);
        component = fixture.componentInstance;
    });

    it('should show spinner when loading', () => {
        component.isTransactionListLoading = true;
        component.transactions = [];
        fixture.detectChanges();

        const spinner = fixture.nativeElement.querySelector('.spinner-border');
        expect(spinner).toBeTruthy();
    });

    it('should show title when provided', () => {
        component.isTransactionListLoading = false;
        component.title = 'Tranzakciók';
        component.transactions = [];
        fixture.detectChanges();

        const titleEl = fixture.nativeElement.querySelector('.title');
        expect(titleEl.textContent.trim()).toBe('Tranzakciók');
    });

    it('should render correct number of transaction cards', () => {
        component.isTransactionListLoading = false;
        component.title = null;
        component.transactions = [
            { id: 1, name: 'tranzakció1', priceSum: 100, transactionType: 'INCOME', transactionDate: '2024-01-01', isComplexTransaction: false, transactionDetails: [] },
            { id: 2, name: 'tranzakció2', priceSum: -200, transactionType: 'OUTCOME', transactionDate: '2024-01-02', isComplexTransaction: true, transactionDetails: [] },
        ];

        fixture.detectChanges();

        const cards = fixture.nativeElement.querySelectorAll('app-transaction-card');
        expect(cards.length).toBe(2);
    });

    it('should emit editTransaction when card is clicked', () => {
        const spy = vi.spyOn(component.editTransaction, 'emit');

        component.isTransactionListLoading = false;
        component.transactions = [
            { id: 5, name: 'tranzakció5', priceSum: 100, transactionType: 'INCOME', transactionDate: '2024-01-01', isComplexTransaction: false, transactionDetails: [] },
        ];

        fixture.detectChanges();

        const card = fixture.nativeElement.querySelector('app-transaction-card');
        card.click();

        expect(spy).toHaveBeenCalledWith({ transactionId: 5 });
    });

    it('should show empty message when no transactions', () => {
        component.isTransactionListLoading = false;
        component.transactions = [];
        fixture.detectChanges();

        const emptyMsg = fixture.nativeElement.querySelector('p.text-center');
        expect(emptyMsg).toBeTruthy();
    });
});