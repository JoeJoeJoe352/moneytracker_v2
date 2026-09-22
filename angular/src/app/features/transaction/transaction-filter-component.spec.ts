import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { provideTranslateService } from '@ngx-translate/core';
import { TransactionFilter } from './transaction-filter-component';
import type { FilterData } from './transactions-component';

describe('TransactionFilter (Vitest)', () => {
    let fixture: ComponentFixture<TransactionFilter>;
    let component: TransactionFilter;
    let submitted: FilterData[];
    let resetCount: number;

    const defaultDate = new Date(2026, 8, 21);

    /**
     * Beállítja a kötelező inputokat, majd lefuttatja az első change detectiont (ngOnInit is ekkor fut)
     */
    async function render(defaultData: FilterData, isLoading = false): Promise<void> {
        fixture.componentRef.setInput('defaultData', defaultData);
        fixture.componentRef.setInput('isTransactionListLoading', isLoading);
        fixture.detectChanges();
        await fixture.whenStable();
    }

    function nameInput(): HTMLInputElement {
        return fixture.nativeElement.querySelector('#name');
    }

    function typeName(value: string): void {
        nameInput().value = value;
        nameInput().dispatchEvent(new Event('input'));
    }

    function submitButton(): HTMLButtonElement {
        return fixture.nativeElement.querySelector('button[type="submit"]');
    }

    function clearButton(): HTMLButtonElement {
        return fixture.nativeElement.querySelector('#clear-filters');
    }

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [TransactionFilter],
            providers: [provideTranslateService(), provideNativeDateAdapter()],
        }).compileComponents();

        fixture = TestBed.createComponent(TransactionFilter);
        component = fixture.componentInstance;

        submitted = [];
        resetCount = 0;
        component.filterSubmit.subscribe((data) => submitted.push(data));
        component.resetForm.subscribe(() => resetCount++);
    });

    it('should fill the name input from the default data', async () => {
        await render({ name: 'tej', date: null });

        expect(nameInput().value).toBe('tej');
    });

    it('should emit the default name and date when submitted without changes', async () => {
        await render({ name: 'tej', date: defaultDate });

        submitButton().click();

        expect(submitted).toEqual([{ name: 'tej', date: defaultDate }]);
    });

    it('should emit the trimmed name when submitted', async () => {
        await render({ name: '', date: null });

        typeName('  kenyér  ');
        submitButton().click();

        expect(submitted).toEqual([{ name: 'kenyér', date: null }]);
    });

    it('should empty the inputs and emit resetForm when the clear button is clicked', async () => {
        await render({ name: 'tej', date: defaultDate });

        clearButton().click();

        expect(nameInput().value).toBe('');
        expect(resetCount).toBe(1);
        // nem a kezdőértékekre áll vissza: a szülő is szűrők nélkül tölti újra a listát
        submitButton().click();
        expect(submitted).toEqual([{ name: '', date: null }]);
    });

    it('should disable both buttons while the transaction list is loading', async () => {
        await render({ name: '', date: null }, true);

        expect(submitButton().disabled).toBe(true);
        expect(clearButton().disabled).toBe(true);
    });

    it('should enable both buttons when the transaction list is not loading', async () => {
        await render({ name: '', date: null }, false);

        expect(submitButton().disabled).toBe(false);
        expect(clearButton().disabled).toBe(false);
    });
});
