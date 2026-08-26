import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormBuilder, FormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { provideTranslateService } from '@ngx-translate/core';
import { signal } from '@angular/core';
import { TransactionDetailRowComponent } from './transaction-detail-row-component';
import { DetailForm } from '../transaction/interfaces';
import { DropdownInterface } from '../../shared/interfaces';

describe('TransactionDetailRowComponent (Vitest)', () => {
    let fixture: ComponentFixture<TransactionDetailRowComponent>;
    let component: TransactionDetailRowComponent;
    const fb = new FormBuilder();

    function buildDetailGroup(overrides: Partial<{
        detailName: string;
        detailPrice: number | null;
        detailWeight: number | null;
        detailUnitPrice: number | null;
        detailIsComplexPriceMode: boolean;
        categories: DropdownInterface[];
    }> = {}): FormGroup<DetailForm> {
        return fb.group({
            detailName: [overrides.detailName ?? 'kenyér'],
            detailPrice: [overrides.detailPrice ?? 500],
            detailWeight: [overrides.detailWeight ?? null],
            detailUnitPrice: [overrides.detailUnitPrice ?? null],
            detailIsComplexPriceMode: [overrides.detailIsComplexPriceMode ?? false],
            categories: [overrides.categories ?? []],
        }) as FormGroup<DetailForm>;
    }

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [TransactionDetailRowComponent],
            providers: [provideTranslateService()],
        }).compileComponents();

        fixture = TestBed.createComponent(TransactionDetailRowComponent);
        component = fixture.componentInstance;
        component.categoryData = signal<DropdownInterface[]>([]);
        component.multiselectSettings = signal({
            singleSelection: false,
            idField: 'item_id',
            textField: 'item_text',
        });
        component.isCategorySaveInProgress = false;
        component.isLastDetailRow = false;
        component.index = 0;
    });

    it('should show simple price input when detailIsComplexPriceMode is false, and hide weight/unitprice', () => {
        component.detail = buildDetailGroup({ detailIsComplexPriceMode: false });

        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('#detail-price-0')).toBeTruthy();
        expect(fixture.nativeElement.querySelector('#detail-weight-0')).toBeNull();
        expect(fixture.nativeElement.querySelector('#detail-unitprice-0')).toBeNull();
    });

    it('should show weight/unitprice/total-price inputs and compute the total when detailIsComplexPriceMode is true', () => {
        component.detail = buildDetailGroup({
            detailIsComplexPriceMode: true,
            detailWeight: 2,
            detailUnitPrice: 300,
        });

        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('#detail-price-0')).toBeNull();
        const weightInput = fixture.nativeElement.querySelector('#detail-weight-0');
        const unitPriceInput = fixture.nativeElement.querySelector('#detail-unitprice-0');
        expect(weightInput.value).toBe('2');
        expect(unitPriceInput.value).toBe('300');

        const suffixInputs = fixture.nativeElement.querySelectorAll('.input-with-suffix input');
        const totalPriceInput = suffixInputs[suffixInputs.length - 1];
        expect(Number(totalPriceInput.value)).toBe(600);
        expect(totalPriceInput.disabled).toBe(true);
    });

    it('should show a required error on the name field only after it becomes touched and invalid', () => {
        component.detail = buildDetailGroup({ detailName: '' });
        component.detail.controls.detailName.setValidators((control) =>
            control.value ? null : { required: true },
        );
        component.detail.controls.detailName.updateValueAndValidity();

        fixture.detectChanges();
        expect(fixture.nativeElement.querySelector('.form-field-error')).toBeNull();

        component.detail.controls.detailName.markAsTouched();
        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('.form-field-error')).toBeTruthy();
    });

    it('should disable the delete button when isLastDetailRow is true', () => {
        component.detail = buildDetailGroup();
        component.isLastDetailRow = true;

        fixture.detectChanges();

        const button = fixture.nativeElement.querySelector('button.btn-primary-red');
        expect(button.disabled).toBe(true);
    });

    it('should enable the delete button when isLastDetailRow is false', () => {
        component.detail = buildDetailGroup();
        component.isLastDetailRow = false;

        fixture.detectChanges();

        const button = fixture.nativeElement.querySelector('button.btn-primary-red');
        expect(button.disabled).toBe(false);
    });

    it('should emit rowDeleted when the delete button is clicked', () => {
        component.detail = buildDetailGroup();
        component.isLastDetailRow = false;
        fixture.detectChanges();

        let emitted = false;
        component.rowDeleted.subscribe(() => (emitted = true));

        const button = fixture.nativeElement.querySelector('button.btn-primary-red');
        button.click();

        expect(emitted).toBe(true);
    });

    it('should forward category dropdown events (filter change, click, dropdown close) to its outputs', () => {
        component.detail = buildDetailGroup();
        fixture.detectChanges();

        const dropdown = fixture.debugElement.query(By.css('ng-multiselect-dropdown'));

        let filterChangeValue: unknown;
        component.categoryFilterChange.subscribe((v) => (filterChangeValue = v));
        dropdown.triggerEventHandler('onFilterChange', 'kenyér');
        expect(filterChangeValue).toBe('kenyér');

        let clickEmitted = false;
        component.categoryDropdownClick.subscribe(() => (clickEmitted = true));
        dropdown.triggerEventHandler('click', new Event('click'));
        expect(clickEmitted).toBe(true);

        let closedEmitted = false;
        component.categorySearchTextCleared.subscribe(() => (closedEmitted = true));
        dropdown.triggerEventHandler('onDropDownClose', undefined);
        expect(closedEmitted).toBe(true);
    });
});
