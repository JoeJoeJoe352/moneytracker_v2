import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideTranslateService } from '@ngx-translate/core';
import { signal } from '@angular/core';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { MatChipRow } from '@angular/material/chips';
import { CategorySelectComponent } from './category-select-component';
import { DropdownInterface } from '../../shared/interfaces';

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
        component.categoryData = signal<DropdownInterface[]>([food, transport]);
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

    it('should emit categoryAdded with the trimmed search text when the "add new" option is selected', () => {
        fixture.detectChanges();
        setSearchText('  Health  ');

        let addedCategory: string | undefined;
        component.categoryAdded.subscribe((name) => (addedCategory = name));

        selectOption(component.addNewOption);

        expect(addedCategory).toBe('Health');
    });

    it('should not emit categoryAdded when disabled', () => {
        fixture.detectChanges();
        component.setDisabledState(true);
        setSearchText('Health');

        let addedCategory: string | undefined;
        component.categoryAdded.subscribe((name) => (addedCategory = name));

        selectOption(component.addNewOption);

        expect(addedCategory).toBeUndefined();
    });

    it('should clear the search text after a selection', () => {
        fixture.detectChanges();
        setSearchText('Foo');

        selectOption(food);
        fixture.detectChanges();

        expect(searchInput().value).toBe('');
    });
});
