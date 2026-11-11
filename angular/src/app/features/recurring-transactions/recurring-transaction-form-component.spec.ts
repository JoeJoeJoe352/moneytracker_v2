import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { provideNativeDateAdapter } from '@angular/material/core';
import { RecurringTransactionFormComponent } from './recurring-transaction-form-component';
import {
    RecurringTransactionResponseInterface,
    RecurringTransactionSaveRequestInterface,
} from './interfaces';
import { CategoryResponseInterface } from '../transaction/interfaces';
import { WalletDataInterface } from '../wallet/interfaces';
import { UserDataStore } from '@shared/stores/user-data-store';
import { TimeHelper } from '@shared/services/time-helper';
import {
    CurrencyCodesEnum,
    RecurrenceIntervalEnum,
    RecurringTransactionStatusEnum,
    TransactionTypeEnum,
    WalletTypesEnum,
} from '@shared/enums';

const wallets: WalletDataInterface[] = [
    {
        id: 1,
        name: 'Test Wallet',
        type: WalletTypesEnum.default,
        currencyCode: CurrencyCodesEnum.huf,
    },
    {
        id: 2,
        name: 'Euro Wallet',
        type: WalletTypesEnum.default,
        currencyCode: CurrencyCodesEnum.eur,
    },
];

const categories: CategoryResponseInterface[] = [
    { id: 10, name: 'Rezsi', isDefaultCategory: false },
    { id: 11, name: 'Lakás', isDefaultCategory: false },
    { id: 12, name: 'Szórakozás', isDefaultCategory: false },
];

/**
 * A mai naphoz képest megadott nappal eltolt dátum (a "nem lehet múltbeli" validáció miatt
 * nem lehet fix dátum a tesztadatokban)
 */
function daysFromToday(days: number): Date {
    const date = new Date();
    date.setHours(0, 0, 0, 0);
    date.setDate(date.getDate() + days);
    return date;
}

/**
 * Az előző hónap utolsó napja: az első olyan nap, ami már nem választható (bármelyik napon fut a teszt)
 */
function lastDayOfPreviousMonth(): Date {
    const today = new Date();
    return new Date(today.getFullYear(), today.getMonth(), 0);
}

function dateString(date: Date): string {
    return new TimeHelper().formatDateToString(date);
}

function recurringTransaction(
    overrides: Partial<RecurringTransactionResponseInterface> = {},
): RecurringTransactionResponseInterface {
    return {
        id: 5,
        name: 'Albérlet',
        priceSum: -150000,
        startDate: '2026-01-01',
        nextOccurrenceDate: dateString(daysFromToday(10)),
        recurrenceInterval: RecurrenceIntervalEnum.MONTHLY,
        transactionType: TransactionTypeEnum.OUTCOME,
        status: RecurringTransactionStatusEnum.ACTIVE,
        categories: [categories[0]!, categories[1]!],
        wallet: wallets[1]!,
        ...overrides,
    };
}

describe('RecurringTransactionFormComponent (Vitest)', () => {
    let fixture: ComponentFixture<RecurringTransactionFormComponent>;
    let component: RecurringTransactionFormComponent;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [RecurringTransactionFormComponent],
            providers: [
                provideTranslateService(),
                provideNativeDateAdapter(),
                {
                    provide: UserDataStore,
                    useValue: {
                        getDefaultWallet: () => wallets[0],
                        getWallets: () => wallets,
                    },
                },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(RecurringTransactionFormComponent);
        component = fixture.componentInstance;
        fixture.componentRef.setInput('isFormDisabled', false);
        fixture.componentRef.setInput('isCategorySaveInProgress', false);
        fixture.componentRef.setInput('categoryList', categories);
        fixture.componentRef.setInput('addCategoryCallback', () => {
            throw new Error('not called in this test');
        });
    });

    /**
     * Gomb keresése a felirata alapján (fordítások nélkül a fordítási kulcs jelenik meg)
     */
    function button(translationKey: string): HTMLButtonElement | null {
        const buttons = Array.from<HTMLButtonElement>(
            fixture.nativeElement.querySelectorAll('button'),
        );
        return buttons.find((b) => b.textContent?.trim() === translationKey) ?? null;
    }

    function submitButton(): HTMLButtonElement {
        return fixture.nativeElement.querySelector('button[type="submit"]');
    }

    function errorTexts(): string[] {
        return Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('mat-error')).map(
            (el) => el.textContent?.trim() ?? '',
        );
    }

    function captureSaved(): { value: RecurringTransactionSaveRequestInterface | undefined } {
        const captured: { value: RecurringTransactionSaveRequestInterface | undefined } = {
            value: undefined,
        };
        component.saved.subscribe((value) => (captured.value = value));
        return captured;
    }

    it('should build an empty form with default values for a new recurring transaction', () => {
        fixture.detectChanges();

        expect(component.name.value).toBe('');
        expect(component.name.invalid).toBe(true);
        expect(component.price.value).toBeNull();
        expect(component.isIncome.value).toBe(false);
        expect(component.walletId.value).toBe(1);
        expect(dateString(component.date.value!)).toBe(dateString(new Date()));
        expect(component.date.valid).toBe(true);
    });

    it('should label the date field as start date for a new recurring transaction', () => {
        fixture.detectChanges();

        expect(fixture.nativeElement.textContent).toContain(
            'recurring_transaction.form.start_date',
        );
        expect(fixture.nativeElement.textContent).not.toContain(
            'recurring_transaction.form.next_occurrence_date',
        );
    });

    it('should only show the save button for a new recurring transaction', () => {
        fixture.detectChanges();

        expect(button('recurring_transaction.form.save')).not.toBeNull();
        expect(button('recurring_transaction.form.delete')).toBeNull();
        expect(button('recurring_transaction.form.stop')).toBeNull();
        expect(button('recurring_transaction.form.restart')).toBeNull();
    });

    it('should not emit "saved" and should mark all controls touched when submitting an invalid form', () => {
        fixture.detectChanges();
        const saved = captureSaved();

        submitButton().click();
        fixture.detectChanges();

        expect(saved.value).toBeUndefined();
        expect(component.name.touched).toBe(true);
        expect(component.price.touched).toBe(true);
        expect(errorTexts()).toContain('transaction.name.required');
        expect(errorTexts()).toContain('transaction.price.required');
    });

    it('should emit "saved" with a negative price and OUTCOME type for an expense', () => {
        fixture.detectChanges();
        const saved = captureSaved();

        component.name.setValue('Netflix');
        component.price.setValue(4990);
        submitButton().click();

        expect(saved.value).toEqual({
            name: 'Netflix',
            priceSum: -4990,
            startDate: dateString(new Date()),
            recurrenceInterval: RecurrenceIntervalEnum.MONTHLY,
            transactionType: TransactionTypeEnum.OUTCOME,
            categoryIds: [],
            walletId: 1,
            isRestart: false,
        });
    });

    it('should emit "saved" with a positive price and INCOME type for an income', () => {
        fixture.detectChanges();
        const saved = captureSaved();

        component.name.setValue('Fizetés');
        component.price.setValue(500000);
        component.isIncome.setValue(true);
        submitButton().click();

        expect(saved.value).toMatchObject({
            priceSum: 500000,
            transactionType: TransactionTypeEnum.INCOME,
        });
    });

    it('should show an error and not emit "saved" when the date is before the current month', () => {
        fixture.detectChanges();
        const saved = captureSaved();

        component.name.setValue('Netflix');
        component.price.setValue(4990);
        component.date.setValue(lastDayOfPreviousMonth());
        submitButton().click();
        fixture.detectChanges();

        expect(component.date.hasError('beforeThisMonth')).toBe(true);
        expect(errorTexts()).toContain('recurring_transaction.form.date_before_this_month');
        expect(saved.value).toBeUndefined();
    });

    it('should accept a future date', () => {
        fixture.detectChanges();
        const saved = captureSaved();

        component.name.setValue('Netflix');
        component.price.setValue(4990);
        component.date.setValue(daysFromToday(30));
        submitButton().click();

        expect(saved.value?.startDate).toBe(dateString(daysFromToday(30)));
    });

    it('should fill the form with the data of the edited recurring transaction', () => {
        const edited = recurringTransaction();
        fixture.componentRef.setInput('recurringTransaction', edited);
        fixture.detectChanges();

        expect(component.name.value).toBe('Albérlet');
        // a kiadás összege a formban pozitív
        expect(component.price.value).toBe(150000);
        expect(component.isIncome.value).toBe(false);
        expect(component.walletId.value).toBe(2);
        // a dátum mező a következő esedékességet mutatja, nem a kezdő dátumot
        expect(dateString(component.date.value!)).toBe(edited.nextOccurrenceDate);
        expect(fixture.nativeElement.textContent).toContain(
            'recurring_transaction.form.next_occurrence_date',
        );
    });

    it('should emit the unchanged data of the edited recurring transaction when saving', () => {
        const edited = recurringTransaction({
            recurrenceInterval: RecurrenceIntervalEnum.WEEKLY,
        });
        fixture.componentRef.setInput('recurringTransaction', edited);
        fixture.detectChanges();
        const saved = captureSaved();

        submitButton().click();

        expect(saved.value).toEqual({
            name: 'Albérlet',
            priceSum: -150000,
            startDate: edited.nextOccurrenceDate,
            recurrenceInterval: RecurrenceIntervalEnum.WEEKLY,
            transactionType: TransactionTypeEnum.OUTCOME,
            categoryIds: [10, 11],
            walletId: 2,
            isRestart: false,
        });
    });

    it('should show the delete and stop buttons for an active recurring transaction', () => {
        fixture.componentRef.setInput('recurringTransaction', recurringTransaction());
        fixture.detectChanges();

        expect(button('recurring_transaction.form.delete')).not.toBeNull();
        expect(button('recurring_transaction.form.stop')).not.toBeNull();
        expect(button('recurring_transaction.form.restart')).toBeNull();
    });

    it('should emit "stopped" when clicking the stop button', () => {
        fixture.componentRef.setInput('recurringTransaction', recurringTransaction());
        fixture.detectChanges();
        let stopped = false;
        component.stopped.subscribe(() => (stopped = true));

        button('recurring_transaction.form.stop')!.click();

        expect(stopped).toBe(true);
    });

    it('should emit "deleted" when clicking the delete button', () => {
        fixture.componentRef.setInput('recurringTransaction', recurringTransaction());
        fixture.detectChanges();
        let deleted = false;
        component.deleted.subscribe(() => (deleted = true));

        button('recurring_transaction.form.delete')!.click();

        expect(deleted).toBe(true);
    });

    it('should show the restart button instead of stop for a stopped recurring transaction', () => {
        fixture.componentRef.setInput(
            'recurringTransaction',
            recurringTransaction({ status: RecurringTransactionStatusEnum.STOPPED }),
        );
        fixture.detectChanges();

        expect(button('recurring_transaction.form.restart')).not.toBeNull();
        expect(button('recurring_transaction.form.stop')).toBeNull();
        expect(button('recurring_transaction.form.delete')).not.toBeNull();
    });

    it('should emit "saved" with isRestart true when clicking the restart button', () => {
        fixture.componentRef.setInput(
            'recurringTransaction',
            recurringTransaction({ status: RecurringTransactionStatusEnum.STOPPED }),
        );
        fixture.detectChanges();
        const saved = captureSaved();

        button('recurring_transaction.form.restart')!.click();

        expect(saved.value).toMatchObject({ name: 'Albérlet', isRestart: true });
    });

    it('should not restart a stopped recurring transaction whose next occurrence is before the current month', () => {
        fixture.componentRef.setInput(
            'recurringTransaction',
            recurringTransaction({
                status: RecurringTransactionStatusEnum.STOPPED,
                nextOccurrenceDate: dateString(lastDayOfPreviousMonth()),
            }),
        );
        fixture.detectChanges();
        const saved = captureSaved();

        button('recurring_transaction.form.restart')!.click();
        fixture.detectChanges();

        expect(saved.value).toBeUndefined();
        expect(errorTexts()).toContain('recurring_transaction.form.date_before_this_month');
    });

    it('should disable the form and all buttons when isFormDisabled is true', () => {
        fixture.componentRef.setInput('recurringTransaction', recurringTransaction());
        fixture.componentRef.setInput('isFormDisabled', true);
        fixture.detectChanges();

        expect(component.name.disabled).toBe(true);
        expect(component.price.disabled).toBe(true);
        expect(submitButton().disabled).toBe(true);
        expect(button('recurring_transaction.form.delete')!.disabled).toBe(true);
        expect(button('recurring_transaction.form.stop')!.disabled).toBe(true);
    });

    it('should enable the form again when isFormDisabled turns false', () => {
        fixture.componentRef.setInput('isFormDisabled', true);
        fixture.detectChanges();
        fixture.componentRef.setInput('isFormDisabled', false);
        fixture.detectChanges();

        expect(component.name.enabled).toBe(true);
        expect(submitButton().disabled).toBe(false);
    });
});
