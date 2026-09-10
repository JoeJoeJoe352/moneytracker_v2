import {
    Component,
    computed,
    ElementRef,
    EventEmitter,
    forwardRef,
    Input,
    Output,
    Signal,
    signal,
    ViewChild,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipsModule } from '@angular/material/chips';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';
import { DropdownInterface } from '../../shared/interfaces';

/**
 * Kiválasztott érték az "új kategória hozzáadása" opcióhoz, hogy megkülönböztethető legyen
 * egy ténylegesen létező kategóriától
 */
const ADD_NEW_OPTION = Symbol('add-new-category');

@Component({
    selector: 'app-category-select',
    templateUrl: './category-select-component.html',
    styleUrl: './category-select-component.scss',
    imports: [
        ReactiveFormsModule,
        MatFormFieldModule,
        MatChipsModule,
        MatAutocompleteModule,
        MatIconModule,
        TranslatePipe,
    ],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => CategorySelectComponent),
            multi: true,
        },
    ],
})
export class CategorySelectComponent implements ControlValueAccessor {
    @ViewChild('categoryInput') private categoryInput?: ElementRef<HTMLInputElement>;

    /**
     * Kiválasztható kategóriák listája
     */
    @Input({ required: true }) categoryData!: Signal<DropdownInterface[]>;
    /**
     * Kategória mentése folyamatban van-e (ez alatt az egész komponens le van tiltva)
     */
    @Input() disabled = false;

    /**
     * Új kategóriát szeretne a user hozzáadni a listájához
     */
    @Output() categoryAdded = new EventEmitter<string>();

    /**
     * Az "új kategória hozzáadása" opció értéke az autocomplete-ban (public, hogy tesztelhető legyen)
     */
    readonly addNewOption = ADD_NEW_OPTION;

    protected searchControl = new FormControl('', { nonNullable: true });

    protected selected = signal<DropdownInterface[]>([]);

    private searchText = signal('');

    protected filteredOptions: Signal<DropdownInterface[]> = computed(() => {
        const search = this.searchText().trim().toLowerCase();
        const selectedIds = new Set(this.selected().map((category) => category.item_id));
        return this.categoryData()
            .filter((category) => !selectedIds.has(category.item_id))
            .filter((category) => category.item_text.toLowerCase().includes(search));
    });

    protected showAddOption: Signal<boolean> = computed(() => {
        const search = this.searchText().trim();
        if (!search) {
            return false;
        }
        return !this.categoryData().some(
            (category) => category.item_text.toLowerCase() === search.toLowerCase(),
        );
    });

    private onChange: (value: DropdownInterface[]) => void = () => undefined;
    private onTouched: () => void = () => undefined;

    constructor() {
        this.searchControl.valueChanges.subscribe((value) => this.searchText.set(value));
    }

    writeValue(value: DropdownInterface[] | null): void {
        this.selected.set(value ?? []);
    }

    registerOnChange(fn: (value: DropdownInterface[]) => void): void {
        this.onChange = fn;
    }

    registerOnTouched(fn: () => void): void {
        this.onTouched = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        this.disabled = isDisabled;
        if (isDisabled) {
            this.searchControl.disable();
        } else {
            this.searchControl.enable();
        }
    }

    /**
     * Autocomplete-ban kiválasztott elem lekezelése: vagy egy meglévő kategóriát választ ki
     * a user, vagy az "új kategória hozzáadása" opciót
     */
    protected optionSelected(event: MatAutocompleteSelectedEvent): void {
        const value = event.option.value as DropdownInterface | typeof ADD_NEW_OPTION;

        if (value === ADD_NEW_OPTION) {
            const name = this.searchText().trim();
            if (name && !this.disabled) {
                this.categoryAdded.emit(name);
            }
        } else {
            this.selected.update((categories) => [...categories, value]);
            this.emitChange();
        }

        this.searchControl.setValue('');
        // a matChipInputFor-ral kombinált input megjelenített értéke nem szinkronizálódik
        // megbízhatóan a FormControl-ból, ezért explicit módon is töröljük (ez a hivatalos
        // Angular Material chips+autocomplete minta is)
        if (this.categoryInput) {
            this.categoryInput.nativeElement.value = '';
        }
    }

    /**
     * Kiválasztott kategória eltávolítása
     */
    protected remove(category: DropdownInterface): void {
        this.selected.update((categories) =>
            categories.filter((selectedCategory) => selectedCategory.item_id !== category.item_id),
        );
        this.emitChange();
    }

    private emitChange(): void {
        this.onChange(this.selected());
        this.onTouched();
    }
}
