import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { MatChipRow } from '@angular/material/chips';
import { CategorySelectComponent } from './category-select-component';
import { DropdownInterface } from '../../shared/interfaces';
import { CategoryResponseInterface } from '../transaction/interfaces';

describe('CategorySelectComponent (Vitest)', () => {
    let fixture: ComponentFixture<CategorySelectComponent>;
    let component: CategorySelectComponent;

    const food: DropdownInterface = { item_id: 1, item_text: 'Food' };
    const transport: DropdownInterface = { item_id: 2, item_text: 'Transport' };

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [CategorySelectComponent],
            providers: [provideTranslateService()],
        }).compileComponents();

        fixture = TestBed.createComponent(CategorySelectComponent);
        component = fixture.componentInstance;
        fixture.componentRef.setInput('categoryData', [food, transport]);
    });

    function chipTexts(): string[] {
        return fixture.debugElement
            .queryAll(By.directive(MatChipRow))
            .map((chip) => chip.nativeElement.textContent.trim());
    }

    function selectOption(value: DropdownInterface | typeof component.addNewOption): void {
        const autocomplete = fixture.debugElement.query(By.directive(MatAutocomplete));
        autocomplete.triggerEventHandler('optionSelected', { option: { value } });
    }

    function searchInput(): HTMLInputElement {
        return fixture.debugElement.query(By.css('input')).nativeElement as HTMLInputElement;
    }

    function setSearchText(text: string): void {
        const input = searchInput();
        input.value = text;
        input.dispatchEvent(new Event('input'));
    }

    it('should render a chip for each pre-selected category via writeValue', () => {
        component.writeValue([food]);
        fixture.detectChanges();

        expect(chipTexts()).toEqual(['Food']);
    });

    it('should render no chips when writeValue is called with null', () => {
        component.writeValue(null);
        fixture.detectChanges();

        expect(chipTexts()).toEqual([]);
    });

    it('should add a category as a chip and notify the value accessor when an option is selected', () => {
        fixture.detectChanges();
        let changedValue: DropdownInterface[] | undefined;
        component.registerOnChange((value) => (changedValue = value));

        selectOption(food);
        fixture.detectChanges();

        expect(chipTexts()).toEqual(['Food']);
        expect(changedValue).toEqual([food]);
    });

    it('should remove a category chip and notify the value accessor', () => {
        component.writeValue([food, transport]);
        fixture.detectChanges();
        let changedValue: DropdownInterface[] | undefined;
        component.registerOnChange((value) => (changedValue = value));

        const removeButtons = fixture.debugElement.queryAll(By.css('button[matChipRemove]'));
        removeButtons[0].nativeElement.click();
        fixture.detectChanges();

        expect(chipTexts()).toEqual(['Transport']);
        expect(changedValue).toEqual([transport]);
    });

    it('should call addCategoryCallback with the trimmed search text and add the returned category as a chip', () => {
        const newCategory: CategoryResponseInterface = {
            id: 3,
            name: 'Health',
            isDefaultCategory: false,
        };
        const addCategoryCallback = vi.fn(() => of(newCategory));
        fixture.componentRef.setInput('addCategoryCallback', addCategoryCallback);
        fixture.detectChanges();
        setSearchText('  Health  ');

        let changedValue: DropdownInterface[] | undefined;
        component.registerOnChange((value) => (changedValue = value));

        selectOption(component.addNewOption);
        fixture.detectChanges();

        expect(addCategoryCallback).toHaveBeenCalledWith('Health');
        expect(chipTexts()).toEqual(['Health']);
        expect(changedValue).toEqual([{ item_id: 3, item_text: 'Health' }]);
    });

    it('should not add a chip and not raise an unhandled error when addCategoryCallback fails', () => {
        // az RxJS a kezeletlen hibát időzítővel dobja el, ezért a timereket mi léptetjük
        vi.useFakeTimers();
        try {
            const addCategoryCallback = vi.fn(() => throwError(() => new Error('save failed')));
            fixture.componentRef.setInput('addCategoryCallback', addCategoryCallback);
            fixture.detectChanges();
            setSearchText('Health');

            selectOption(component.addNewOption);
            fixture.detectChanges();

            expect(() => vi.runAllTimers()).not.toThrow();
            expect(chipTexts()).toEqual([]);
        } finally {
            vi.useRealTimers();
        }
    });

    it('should not call addCategoryCallback when disabled', () => {
        const addCategoryCallback = vi.fn(() =>
            of<CategoryResponseInterface>({ id: 3, name: 'Health', isDefaultCategory: false }),
        );
        fixture.componentRef.setInput('addCategoryCallback', addCategoryCallback);
        fixture.detectChanges();
        component.setDisabledState(true);
        setSearchText('Health');

        selectOption(component.addNewOption);

        expect(addCategoryCallback).not.toHaveBeenCalled();
    });

    it('should clear the search text after a selection', () => {
        fixture.detectChanges();
        setSearchText('Foo');

        selectOption(food);
        fixture.detectChanges();

        expect(searchInput().value).toBe('');
    });
});
