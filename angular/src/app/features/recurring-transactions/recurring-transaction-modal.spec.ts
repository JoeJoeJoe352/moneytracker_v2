import { describe, it, expect, vi, afterEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, input, output, signal, WritableSignal } from '@angular/core';
import { Observable, of, Subject, throwError } from 'rxjs';
import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DialogCloseButton } from '@shared/components/mat-modal-close';
import { NotificationService } from '@shared/services/notification-service';
import { CategoryService } from '../transaction/category-service';
import { CategoryResponseInterface } from '../transaction/interfaces';
import { RecurringTransactionModalComponent } from './recurring-transaction-modal';
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

/**
 * A mentést/leállítást/törlést a hívó (oldal) végzi, azt a
 * recurring-transactions-page-component.spec.ts teszteli. Itt csak az események
 * továbbítását és a kategóriák kezelését
 */
describe('RecurringTransactionModalComponent (Vitest)', () => {
    let fixture: ComponentFixture<RecurringTransactionModalComponent>;
    let categoryServiceMock: {
        listCategories: ReturnType<typeof vi.fn>;
        saveCategory: ReturnType<typeof vi.fn>;
    };
    let notificationMock: {
        show: ReturnType<typeof vi.fn>;
        showGeneralError: ReturnType<typeof vi.fn>;
    };
    let isSaving: WritableSignal<boolean>;

    afterEach(() => {
        vi.restoreAllMocks();
    });

    async function setup(
        recurringTransaction: RecurringTransactionResponseInterface | null,
        listCategoriesResult: Observable<CategoryResponseInterface[]> = of(categories),
        waitForStable = true,
    ) {
        categoryServiceMock = {
            listCategories: vi.fn(() => listCategoriesResult),
            saveCategory: vi.fn((data: { name: string }) =>
                of({ id: 99, name: data.name, isDefaultCategory: false }),
            ),
        };
        notificationMock = { show: vi.fn(), showGeneralError: vi.fn() };
        isSaving = signal(false);

        TestBed.configureTestingModule({
            imports: [RecurringTransactionModalComponent],
            providers: [
                provideTranslateService(),
                { provide: MAT_DIALOG_DATA, useValue: { recurringTransaction, isSaving } },
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

    it('should emit saveRequested with the payload when the form is saved', async () => {
        await setup(editedTransaction);
        const emitted: RecurringTransactionSaveRequestInterface[] = [];
        fixture.componentInstance.saveRequested.subscribe((data) => emitted.push(data));

        form()!.saved.emit(payload);

        expect(emitted).toEqual([payload]);
    });

    it('should emit stopRequested when the form is stopped', async () => {
        await setup(editedTransaction);
        const stopSpy = vi.fn();
        fixture.componentInstance.stopRequested.subscribe(stopSpy);

        form()!.stopped.emit();

        expect(stopSpy).toHaveBeenCalledTimes(1);
    });

    it('should emit deleteRequested when the form is deleted', async () => {
        await setup(editedTransaction);
        const deleteSpy = vi.fn();
        fixture.componentInstance.deleteRequested.subscribe(deleteSpy);

        form()!.deleted.emit();

        expect(deleteSpy).toHaveBeenCalledTimes(1);
    });

    it('should disable the form while the caller is saving', async () => {
        await setup(editedTransaction);
        expect(form()!.isFormDisabled()).toBe(false);

        isSaving.set(true);
        fixture.detectChanges();
        expect(form()!.isFormDisabled()).toBe(true);

        isSaving.set(false);
        fixture.detectChanges();
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
