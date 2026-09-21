import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import TransactionListComponent from './transaction-list-component';
import TransactionCardComponent from './transaction-card-component';
import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';
import { CurrencyCodesEnum, TransactionTypeEnum, WalletTypesEnum } from '../../shared/enums';

const sampleWallet = {
    id: 1,
    name: 'Napi költés',
    currencyCode: CurrencyCodesEnum.huf,
    type: WalletTypesEnum.default,
};

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
        fixture.componentRef.setInput('isTransactionListLoading', true);
        fixture.componentRef.setInput('transactions', []);
        fixture.detectChanges();

        const spinner = fixture.nativeElement.querySelector('mat-spinner');
        expect(spinner).toBeTruthy();
    });

    it('should show title when provided', () => {
        fixture.componentRef.setInput('isTransactionListLoading', false);
        fixture.componentRef.setInput('title', 'Tranzakciók');
        fixture.componentRef.setInput('transactions', []);
        fixture.detectChanges();

        const titleEl = fixture.nativeElement.querySelector('[mat-card-title]');
        expect(titleEl.textContent.trim()).toBe('Tranzakciók');
    });

    it('should render correct number of transaction cards', () => {
        fixture.componentRef.setInput('isTransactionListLoading', false);
        fixture.componentRef.setInput('title', null);
        fixture.componentRef.setInput('transactions', [
            { id: 1, name: 'tranzakció1', priceSum: 100, transactionType: TransactionTypeEnum.INCOME, transactionDate: '2024-01-01', isComplexTransaction: false, transactionDetails: [], wallet: sampleWallet },
            { id: 2, name: 'tranzakció2', priceSum: -200, transactionType: TransactionTypeEnum.OUTCOME, transactionDate: '2024-01-02', isComplexTransaction: true, transactionDetails: [], wallet: sampleWallet },
        ]);

        fixture.detectChanges();

        const cards = fixture.nativeElement.querySelectorAll('app-transaction-card');
        expect(cards.length).toBe(2);
    });

    it('should emit editTransaction when card is clicked', () => {
        const spy = vi.spyOn(component.editTransaction, 'emit');

        fixture.componentRef.setInput('isTransactionListLoading', false);
        fixture.componentRef.setInput('transactions', [
            { id: 5, name: 'tranzakció5', priceSum: 100, transactionType: TransactionTypeEnum.INCOME, transactionDate: '2024-01-01', isComplexTransaction: false, transactionDetails: [], wallet: sampleWallet },
        ]);

        fixture.detectChanges();

        const card = fixture.nativeElement.querySelector('app-transaction-card');
        card.click();

        expect(spy).toHaveBeenCalledWith({ transactionId: 5 });
    });

    it('should show empty message when no transactions', () => {
        fixture.componentRef.setInput('isTransactionListLoading', false);
        fixture.componentRef.setInput('transactions', []);
        fixture.detectChanges();

        const emptyMsg = fixture.nativeElement.querySelector('p.text-center');
        expect(emptyMsg).toBeTruthy();
    });
});