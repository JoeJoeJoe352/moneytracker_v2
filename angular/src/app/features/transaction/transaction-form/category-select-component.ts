import {
    Component,
    computed,
    ElementRef,
    forwardRef,
    input,
    Signal,
    signal,
    viewChild,
} from '@angular/core';
import {
    ControlValueAccessor,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule,
    FormControl,
} from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipsModule } from '@angular/material/chips';
import {
    MatAutocompleteModule,
    MatAutocompleteSelectedEvent,
} from '@angular/material/autocomplete';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';
import { DropdownInterface } from '@shared/interfaces';
import { Observable } from 'rxjs';
import { CategoryResponseInterface } from '../interfaces';
import { toSignal } from '@angular/core/rxjs-interop';

/**
 * Kiválasztott érték az "új kategória hozzáadása" opcióhoz, hogy megkülönböztethető legyen
 * egy ténylegesen létező kategóriától
 */
const ADD_NEW_OPTION_SYMBOL = Symbol('add-new-category');

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
    private readonly categoryInput =
        viewChild.required<ElementRef<HTMLInputElement>>('categoryInput');

    /**
     * Kiválasztható kategóriák listája
     */
    public categoryData = input.required<DropdownInterface[]>();
    /**
     * Kategória mentése folyamatban van-e (ez alatt az egész komponens le van tiltva)
     */
    public disabled = input(false);
    /**
     * A form (ControlValueAccessor.setDisabledState) letiltotta-e a mezőt
     */
    private formDisabled = signal(false);
    /**
     * Ténylegesen le van-e tiltva a komponens (a disabled input vagy a form letiltása miatt)
     */
    protected isDisabled = computed(() => this.disabled() || this.formDisabled());

    public addCategoryCallback =
        input.required<(name: string) => Observable<CategoryResponseInterface>>();

    private onChange: (value: DropdownInterface[]) => void = () => undefined;

    private onTouched: () => void = () => undefined;

    protected searchControl = new FormControl('', { nonNullable: true });

    /**
     * Inputba írt keresési szöveg
     */
    private searchText = toSignal(this.searchControl.valueChanges, { initialValue: '' });

    protected selected = signal<DropdownInterface[]>([]);

    /**
     * Az "új kategória hozzáadása" opció értéke az autocomplete-ban (public, hogy tesztelhető legyen)
     */
    public readonly addNewOption = ADD_NEW_OPTION_SYMBOL;

    /**
     * Opciók a selecthez. Kiszűrve azok az elemek, amik már benne vannak a listában és a keresési szövegnek megfelelnek
     */
    protected filteredOptions: Signal<DropdownInterface[]> = computed(() => {
        const search = this.searchText().trim().toLowerCase();
        const selectedIds = new Set(this.selected().map((category) => category.item_id));
        return this.categoryData()
            .filter((category) => !selectedIds.has(category.item_id))
            .filter((category) => category.item_text.toLowerCase().includes(search));
    });

    /**
     * Megjelenjen-e az új opció hozzáadása gomb. Az összes elem listáját kell nézni ilyenkor,
     * nehogy fel tudja venni a user ugyanazt, amit már egyszer kiválasztott
     */
    protected showAddOption: Signal<boolean> = computed(() => {
        const search = this.searchText().trim();
        if (!search) {
            return false;
        }
        return !this.categoryData().some(
            (category) => category.item_text.toLowerCase() === search.toLowerCase(),
        );
    });

    /**
     * Autocomplete-ban kiválasztott elem lekezelése: vagy egy meglévő kategóriát választ ki
     * a user, vagy az "új kategória hozzáadása" opciót
     */
    protected optionSelected(event: MatAutocompleteSelectedEvent): void {
        const value = event.option.value as DropdownInterface | typeof ADD_NEW_OPTION_SYMBOL;

        if (value === ADD_NEW_OPTION_SYMBOL) {
            // nyers input adatokból olvassuk ki, mert a this.searchText()-be ilyenkor a symbol kerül be
            const name = this.categoryInput().nativeElement.value.trim();
            if (name && !this.isDisabled()) {
                this.addCategoryCallback()(name).subscribe({
                    next: (category) => {
                        // note: új kategóriát a szülő state service-ben adjuk hozzá
                        this.selected.update((categories) => [
                            ...categories,
                            { item_id: category.id, item_text: category.name },
                        ]);
                        this.emitChange();
                    },
                    // A hibát a callback már kezelte, itt csak azt előzzük meg, hogy kezeletlen RxJS hibaként felszínre kerüljön
                    error: () => undefined,
                });
            }
        } else {
            this.selected.update((categories) => [...categories, value]);
            this.emitChange();
        }

        this.searchControl.setValue('');
        // Az input megjelenített értéke nem szinkronizálódik a FormControl-ból (matChipInputFor miatt),
        // ezért kézzel is töröljük
        this.categoryInput().nativeElement.value = '';
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

    // ControlValueAccessor implementációk

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
        this.formDisabled.set(isDisabled);
        if (isDisabled) {
            this.searchControl.disable();
        } else {
            this.searchControl.enable();
        }
    }
}
