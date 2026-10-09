import { describe, it, expect } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, input, signal } from '@angular/core';
import { of } from 'rxjs';
import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DialogCloseButton } from '@shared/components/mat-modal-close';
import { TransactionModalComponent, TransactionModalInputInterface } from './transaction-modal';
import { CategoryResponseInterface, TransactionDataFromBackend } from './interfaces';
import { TransactionSpecialTypeEnum, TransactionTypeEnum } from '@shared/enums';

@Component({
    selector: 'app-transaction-form-component',
    template: '',
})
class StubTransactionFormComponent {
    isTransactionFormDisabled = input(false);
    categoryList = input<CategoryResponseInterface[]>([]);
    transaction = input<TransactionDataFromBackend | null>(null);
    addCategoryCallback = input<unknown>();
    isCategorySaveInProgress = input(false);
}

const baseTransaction: TransactionDataFromBackend = {
    id: 1,
    name: 'Szinkronizálás: wallet',
    priceSum: -10,
    transactionType: TransactionTypeEnum.OUTCOME,
    transactionDate: '2026-10-01',
    isComplexTransaction: false,
    transactionDetails: [],
    walletId: 1,
    specialType: null,
};

describe('TransactionModalComponent (Vitest)', () => {
    let fixture: ComponentFixture<TransactionModalComponent>;

    async function setup(transaction: TransactionDataFromBackend | null) {
        TestBed.configureTestingModule({
            imports: [TransactionModalComponent],
            providers: [
                provideTranslateService(),
                {
                    provide: MAT_DIALOG_DATA,
                    useValue: {
                        transaction: signal(transaction),
                        categories: signal([]),
                        isTransactionFormDisabled: signal(false),
                        isCategorySaveInProgress: signal(false),
                        isDataInitializing: signal(false),
                        addCategoryCallback: () => of({} as CategoryResponseInterface),
                    } as TransactionModalInputInterface,
                },
            ],
        });
        TestBed.overrideComponent(TransactionModalComponent, {
            set: {
                imports: [
                    StubTransactionFormComponent,
                    TranslatePipe,
                    MatDialogModule,
                    MatProgressSpinnerModule,
                    DialogCloseButton,
                ],
            },
        });

        fixture = TestBed.createComponent(TransactionModalComponent);
        fixture.detectChanges();
        await fixture.whenStable();
    }

    function warningBlock(): HTMLElement | null {
        return fixture.nativeElement.querySelector('.warning-block');
    }

    it('should show the sync warning for a sync transaction', async () => {
        await setup({ ...baseTransaction, specialType: TransactionSpecialTypeEnum.SYNC });

        expect(warningBlock()?.textContent).toContain('transaction.is_sync_transaction');
    });

    it('should not show the sync warning for a normal transaction', async () => {
        await setup(baseTransaction);

        expect(warningBlock()).toBeNull();
    });

    it('should not show the sync warning for a recurring transaction', async () => {
        await setup({ ...baseTransaction, specialType: TransactionSpecialTypeEnum.RECURRING });

        expect(warningBlock()).toBeNull();
    });

    it('should not show the sync warning when creating a new transaction', async () => {
        await setup(null);

        expect(warningBlock()).toBeNull();
    });
});
