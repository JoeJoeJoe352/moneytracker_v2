import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormBuilder, FormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { provideTranslateService } from '@ngx-translate/core';
import { signal } from '@angular/core';
import { TransactionDetailFormComponent } from './transaction-detail-form-component';
import { CategorySelectComponent } from './category-select-component';
import { DetailForm } from '../transaction/interfaces';
import { DropdownInterface } from '../../shared/interfaces';

describe('TransactionDetailRowComponent (Vitest)', () => {
    let fixture: ComponentFixture<TransactionDetailFormComponent>;
    let component: TransactionDetailFormComponent;
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
            imports: [TransactionDetailFormComponent],
            providers: [provideTranslateService()],
        }).compileComponents();

        fixture = TestBed.createComponent(TransactionDetailFormComponent);
        component = fixture.componentInstance;
        component.categoryData = signal<DropdownInterface[]>([]);
        component.isCategorySaveInProgress = false;
        component.isLastDetailRow = false;
        component.index = 0;
        component.currencySymbol = 'Ft';
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

        const suffixInputs = fixture.nativeElement.querySelectorAll('mat-form-field input');
        const totalPriceInput = suffixInputs[suffixInputs.length - 1];
        expect(Number(totalPriceInput.value)).toBe(600);
        expect(totalPriceInput.disabled).toBe(true);
    });

    it('should render the currencySymbol input in the simple price suffix', () => {
        component.detail = buildDetailGroup({ detailIsComplexPriceMode: false });
        component.currencySymbol = '€';

        fixture.detectChanges();

        const suffix = fixture.nativeElement.querySelector('[matTextSuffix]');
        expect(suffix.textContent.trim()).toBe('€');
    });

    it('should render the currencySymbol input in the unitprice and total-price suffixes when in complex price mode', () => {
        component.detail = buildDetailGroup({
            detailIsComplexPriceMode: true,
            detailWeight: 2,
            detailUnitPrice: 300,
        });
        component.currencySymbol = '$';

        fixture.detectChanges();

        const suffixes = fixture.nativeElement.querySelectorAll('[matTextSuffix]');
        const suffixTexts = Array.from(suffixes as NodeListOf<HTMLElement>).map((el) =>
            el.textContent.trim(),
        );
        expect(suffixTexts).toEqual(['kg', '$/Kg', '$']);
    });

    it('should show a required error on the name field only after it becomes touched and invalid', () => {
        component.detail = buildDetailGroup({ detailName: '' });
        component.detail.controls.detailName.setValidators((control) =>
            control.value ? null : { required: true },
        );
        component.detail.controls.detailName.updateValueAndValidity();

        fixture.detectChanges();
        expect(fixture.nativeElement.querySelector('mat-error')).toBeNull();

        component.detail.controls.detailName.markAsTouched();
        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('mat-error')).toBeTruthy();
    });

    it('should disable the delete button when isLastDetailRow is true', () => {
        component.detail = buildDetailGroup();
        component.isLastDetailRow = true;

        fixture.detectChanges();

        const button = fixture.nativeElement.querySelector('button.mat-button-danger');
        expect(button.disabled).toBe(true);
    });

    it('should enable the delete button when isLastDetailRow is false', () => {
        component.detail = buildDetailGroup();
        component.isLastDetailRow = false;

        fixture.detectChanges();

        const button = fixture.nativeElement.querySelector('button.mat-button-danger');
        expect(button.disabled).toBe(false);
    });

    it('should emit rowDeleted when the delete button is clicked', () => {
        component.detail = buildDetailGroup();
        component.isLastDetailRow = false;
        fixture.detectChanges();

        let emitted = false;
        component.rowDeleted.subscribe(() => (emitted = true));

        const button = fixture.nativeElement.querySelector('button.mat-button-danger');
        button.click();

        expect(emitted).toBe(true);
    });

    it('should forward categoryAdded from the category select up to its own output', () => {
        component.detail = buildDetailGroup();
        fixture.detectChanges();

        let addedCategory: string | undefined;
        component.categoryAdded.subscribe((name) => (addedCategory = name));

        const categorySelect = fixture.debugElement.query(By.directive(CategorySelectComponent));
        categorySelect.triggerEventHandler('categoryAdded', 'kenyér');

        expect(addedCategory).toBe('kenyér');
    });
});
