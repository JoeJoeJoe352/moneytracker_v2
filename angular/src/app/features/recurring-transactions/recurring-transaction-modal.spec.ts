import { describe, it, expect, vi, afterEach, MockInstance } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, input, output } from '@angular/core';
import { Observable, of, Subject, throwError } from 'rxjs';
import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DialogCloseButton } from '@shared/components/mat-modal-close';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog-component';
import { NotificationService } from '@shared/services/notification-service';
import { CategoryService } from '../transaction/category-service';
import { CategoryResponseInterface } from '../transaction/interfaces';
import { RecurringTransactionModalComponent } from './recurring-transaction-modal';
import { RecurringTransactionService } from './recurring-transactions-service';
import {
    RecurringTransactionResponseInterface,
    RecurringTransactionSaveRequestInterface,
} from './interfaces';
import {
    CurrencyCodesEnum,
    RecurrenceIntervalEnum,
    RecurringTransactionStatusEnum,
    TransactionTypeEnum,
    WalletTypesEnum,
} from '@shared/enums';

@Component({
    selector: 'app-recurring-transaction-form-component',
    template: '',
})
class StubRecurringTransactionFormComponent {
    isFormDisabled = input(false);
    categoryList = input<CategoryResponseInterface[]>([]);
    recurringTransaction = input<RecurringTransactionResponseInterface | null>(null);
    isCategorySaveInProgress = input(false);
    addCategoryCallback = input<unknown>();
    saved = output<RecurringTransactionSaveRequestInterface>();
    deleted = output<void>();
    stopped = output<void>();
}

const categories: CategoryResponseInterface[] = [
    { id: 10, name: 'Rezsi', isDefaultCategory: false },
];

const editedTransaction: RecurringTransactionResponseInterface = {
    id: 5,
    name: 'Albérlet',
    priceSum: -150000,
    startDate: '2026-01-01',
    nextOccurrenceDate: '2026-12-01',
    recurrenceInterval: RecurrenceIntervalEnum.MONTHLY,
    transactionType: TransactionTypeEnum.OUTCOME,
    status: RecurringTransactionStatusEnum.ACTIVE,
    categories: [],
    wallet: {
        id: 1,
        name: 'Napi költés',
        currencyCode: CurrencyCodesEnum.huf,
        type: WalletTypesEnum.default,
    },
};

const payload: RecurringTransactionSaveRequestInterface = {
    name: 'Albérlet',
    priceSum: -150000,
    startDate: '2026-12-01',
    recurrenceInterval: RecurrenceIntervalEnum.MONTHLY,
    transactionType: TransactionTypeEnum.OUTCOME,
    categoryIds: [],
    walletId: 1,
    isRestart: false,
};

describe('RecurringTransactionModalComponent (Vitest)', () => {
    let fixture: ComponentFixture<RecurringTransactionModalComponent>;
    let serviceMock: {
        createRecurringTransaction: ReturnType<typeof vi.fn>;
        updateRecurringTransaction: ReturnType<typeof vi.fn>;
        stopRecurringTransaction: ReturnType<typeof vi.fn>;
        deleteRecurringTransaction: ReturnType<typeof vi.fn>;
    };
    let categoryServiceMock: {
        listCategories: ReturnType<typeof vi.fn>;
        saveCategory: ReturnType<typeof vi.fn>;
    };
    let notificationMock: {
        show: ReturnType<typeof vi.fn>;
        showGeneralError: ReturnType<typeof vi.fn>;
    };
    let dialogRefMock: { close: ReturnType<typeof vi.fn> };
    let confirmOpenSpy: MockInstance<typeof ConfirmDialogComponent.open>;
    let confirmResult: Subject<boolean | undefined>;

    afterEach(() => {
        vi.restoreAllMocks();
    });

    async function setup(
        recurringTransaction: RecurringTransactionResponseInterface | null,
        listCategoriesResult: Observable<CategoryResponseInterface[]> = of(categories),
        waitForStable = true,
    ) {
        serviceMock = {
            createRecurringTransaction: vi.fn(() => of(undefined)),
            updateRecurringTransaction: vi.fn(() => of(undefined)),
            stopRecurringTransaction: vi.fn(() => of(undefined)),
            deleteRecurringTransaction: vi.fn(() => of(undefined)),
        };
        categoryServiceMock = {
            listCategories: vi.fn(() => listCategoriesResult),
            saveCategory: vi.fn((data: { name: string }) =>
                of({ id: 99, name: data.name, isDefaultCategory: false }),
            ),
        };
        notificationMock = { show: vi.fn(), showGeneralError: vi.fn() };
        dialogRefMock = { close: vi.fn() };
        confirmResult = new Subject<boolean | undefined>();
        // A modal a MatDialogModule importja miatt saját MatDialog példányt kap, ezért nem a
        // MatDialog-ot mockoljuk, hanem a megerősítő dialog megnyitását
        confirmOpenSpy = vi.spyOn(ConfirmDialogComponent, 'open').mockReturnValue({
            afterClosed: () => confirmResult.asObservable(),
        } as ReturnType<typeof ConfirmDialogComponent.open>);

        TestBed.configureTestingModule({
            imports: [RecurringTransactionModalComponent],
            providers: [
                provideTranslateService(),
                { provide: MAT_DIALOG_DATA, useValue: { recurringTransaction } },
                { provide: MatDialogRef, useValue: dialogRefMock },
                { provide: RecurringTransactionService, useValue: serviceMock },
                { provide: CategoryService, useValue: categoryServiceMock },
                { provide: NotificationService, useValue: notificationMock },
            ],
        });
        TestBed.overrideComponent(RecurringTransactionModalComponent, {
            set: {
                imports: [
                    StubRecurringTransactionFormComponent,
                    TranslatePipe,
                    MatDialogModule,
                    MatProgressSpinnerModule,
                    DialogCloseButton,
                ],
            },
        });

        fixture = TestBed.createComponent(RecurringTransactionModalComponent);
        fixture.detectChanges();
        // függőben lévő resource mellett a whenStable() nem térne vissza
        if (waitForStable) {
            await fixture.whenStable();
            fixture.detectChanges();
        }
    }

    function form(): StubRecurringTransactionFormComponent | null {
        return (
            (fixture.debugElement.query(
                (de) => de.componentInstance instanceof StubRecurringTransactionFormComponent,
            )?.componentInstance as StubRecurringTransactionFormComponent | undefined) ?? null
        );
    }

    function title(): string {
        return fixture.nativeElement.querySelector('h2').textContent.trim();
    }

    it('should show the create title and pass null to the form for a new recurring transaction', async () => {
        await setup(null);

        expect(title()).toBe('recurring_transaction.form.create');
        expect(form()!.recurringTransaction()).toBeNull();
    });

    it('should show the update title and pass the edited recurring transaction to the form', async () => {
        await setup(editedTransaction);

        expect(title()).toBe('recurring_transaction.form.update');
        expect(form()!.recurringTransaction()).toEqual(editedTransaction);
    });

    it('should show a spinner until the categories are loaded, then pass them to the form', async () => {
        const categoriesSubject = new Subject<CategoryResponseInterface[]>();
        await setup(null, categoriesSubject.asObservable(), false);

        expect(fixture.nativeElement.querySelector('mat-spinner')).not.toBeNull();
        expect(form()).toBeNull();

        categoriesSubject.next(categories);
        categoriesSubject.complete();
        await fixture.whenStable();
        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('mat-spinner')).toBeNull();
        expect(form()!.categoryList()).toEqual(categories);
    });

    it('should show a general error and an empty category list when loading the categories fails', async () => {
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        await setup(
            null,
            throwError(() => new Error('network')),
        );

        expect(notificationMock.showGeneralError).toHaveBeenCalled();
        expect(form()!.categoryList()).toEqual([]);
    });

    it('should create a new recurring transaction, notify and close with true on save', async () => {
        await setup(null);

        form()!.saved.emit(payload);

        expect(serviceMock.createRecurringTransaction).toHaveBeenCalledWith(payload);
        expect(serviceMock.updateRecurringTransaction).not.toHaveBeenCalled();
        expect(notificationMock.show).toHaveBeenCalledWith('etc.save-success');
        expect(dialogRefMock.close).toHaveBeenCalledWith(true);
    });

    it('should update the edited recurring transaction on save', async () => {
        await setup(editedTransaction);

        form()!.saved.emit(payload);

        expect(serviceMock.updateRecurringTransaction).toHaveBeenCalledWith(5, payload);
        expect(serviceMock.createRecurringTransaction).not.toHaveBeenCalled();
        expect(dialogRefMock.close).toHaveBeenCalledWith(true);
    });

    it('should disable the form while saving', async () => {
        const saveSubject = new Subject<void>();
        await setup(null);
        serviceMock.createRecurringTransaction.mockReturnValue(saveSubject.asObservable());

        form()!.saved.emit(payload);
        fixture.detectChanges();

        expect(form()!.isFormDisabled()).toBe(true);

        saveSubject.next();
        saveSubject.complete();
        fixture.detectChanges();

        expect(dialogRefMock.close).toHaveBeenCalledWith(true);
    });

    it('should keep the modal open and enable the form again when saving fails', async () => {
        await setup(null);
        serviceMock.createRecurringTransaction.mockReturnValue(
            throwError(() => new Error('server error')),
        );

        form()!.saved.emit(payload);
        fixture.detectChanges();

        expect(dialogRefMock.close).not.toHaveBeenCalled();
        expect(form()!.isFormDisabled()).toBe(false);
    });

    it('should stop the edited recurring transaction, notify and close with true', async () => {
        await setup(editedTransaction);

        form()!.stopped.emit();

        expect(serviceMock.stopRecurringTransaction).toHaveBeenCalledWith(5);
        expect(notificationMock.show).toHaveBeenCalledWith(
            'recurring_transaction.form.stop-success',
        );
        expect(dialogRefMock.close).toHaveBeenCalledWith(true);
    });

    it('should keep the modal open when stopping fails', async () => {
        await setup(editedTransaction);
        serviceMock.stopRecurringTransaction.mockReturnValue(
            throwError(() => new Error('server error')),
        );

        form()!.stopped.emit();
        fixture.detectChanges();

        expect(dialogRefMock.close).not.toHaveBeenCalled();
        expect(form()!.isFormDisabled()).toBe(false);
    });

    it('should ask for confirmation before deleting', async () => {
        await setup(editedTransaction);

        form()!.deleted.emit();

        expect(confirmOpenSpy).toHaveBeenCalledWith(expect.anything(), {
            message: 'recurring_transaction.form.delete_confirm',
            danger: true,
        });
        expect(serviceMock.deleteRecurringTransaction).not.toHaveBeenCalled();
    });

    it('should delete the recurring transaction, notify and close with true after confirming', async () => {
        await setup(editedTransaction);

        form()!.deleted.emit();
        confirmResult.next(true);

        expect(serviceMock.deleteRecurringTransaction).toHaveBeenCalledWith(5);
        expect(notificationMock.show).toHaveBeenCalledWith('etc.delete-success');
        expect(dialogRefMock.close).toHaveBeenCalledWith(true);
    });

    it('should not delete when the confirmation is cancelled', async () => {
        await setup(editedTransaction);

        form()!.deleted.emit();
        confirmResult.next(false);
        confirmResult.next(undefined);

        expect(serviceMock.deleteRecurringTransaction).not.toHaveBeenCalled();
        expect(dialogRefMock.close).not.toHaveBeenCalled();
    });

    it('should keep the modal open and enable the form again when deleting fails', async () => {
        await setup(editedTransaction);
        serviceMock.deleteRecurringTransaction.mockReturnValue(
            throwError(() => new Error('server error')),
        );

        form()!.deleted.emit();
        confirmResult.next(true);
        fixture.detectChanges();

        expect(dialogRefMock.close).not.toHaveBeenCalled();
        expect(form()!.isFormDisabled()).toBe(false);
    });

    it('should add a new category to the list passed to the form', async () => {
        await setup(null);
        const addCategory = form()!.addCategoryCallback() as (
            name: string,
        ) => Observable<CategoryResponseInterface>;

        addCategory('Új kategória').subscribe();
        fixture.detectChanges();

        expect(categoryServiceMock.saveCategory).toHaveBeenCalledWith({ name: 'Új kategória' });
        expect(form()!.categoryList()).toEqual([
            ...categories,
            { id: 99, name: 'Új kategória', isDefaultCategory: false },
        ]);
        expect(form()!.isCategorySaveInProgress()).toBe(false);
    });
});
