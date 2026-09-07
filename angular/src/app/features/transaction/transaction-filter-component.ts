import {
    Component,
    EventEmitter,
    inject,
    Input,
    OnInit,
    Output,
    WritableSignal,
} from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe } from '@ngx-translate/core';
import type { FilterData } from './transactions-component';

interface FilterFormInterface {
    name: FormControl<string>;
    date: FormControl<Date | null>;
}

@Component({
    selector: 'app-transaction-filter',
    templateUrl: 'transaction-filter-component.html',
    styleUrl: './transaction-filter-component.scss',
    imports: [
        MatCardModule,
        TranslatePipe,
        MatFormFieldModule,
        MatInputModule,
        MatDatepickerModule,
        MatButtonModule,
        ReactiveFormsModule,
    ],
})
export class TransactionFilter implements OnInit {
    private fb = inject(FormBuilder);

    /**
     * Form alapértelmezett adatai
     */
    @Input({ required: true }) defaultData!: FilterData | null;
    /**
     * Tranzakciós adatok töltődnek-e
     */
    @Input({ required: true }) isTransactionListLoading!: WritableSignal<boolean>;
    /**
     * Form elküldése event
     */
    @Output() filterSubmit = new EventEmitter<FilterData>();
    /**
     * Form resetelése event
     */
    @Output() resetForm = new EventEmitter<void>();

    /**
     * Form definiciója
     */
    protected filterForm!: FormGroup<FilterFormInterface>;

    private defaultFormData = { name: '', date: null };

    ngOnInit(): void {
        this.buildForm(this.defaultData ?? this.defaultFormData);
    }

    /**
     * Filter formot létrehozza és beállítja az alapadatait
     */
    private buildForm(defaultData: FilterData): void {
        this.filterForm = this.fb.nonNullable.group({
            name: [defaultData.name],
            date: this.fb.control<Date | null>(defaultData.date),
        });
    }

    /**
     * keresési adatok resetelése
     */
    protected clearInputs(): void {
        // ha volt kezdő paraméter, arra resetelné vissza, ezért kell a defaultFormData paraméter
        this.filterForm.reset(this.defaultFormData);
        this.resetForm.emit();
    }

    /**
     * Form küldése
     */
    protected submitForm(): void {
        this.filterSubmit.emit(this.getValuesFromFilterInputs());
    }

    /**
     * A filter inputok értékei alapján létrehoz egy URLSearchParams objektumot
     */
    private getValuesFromFilterInputs(): FilterData {
        const nameInputValue = this.filterForm.get(['name'])!.value.trim() as string;
        const dateInputValue = this.filterForm.get(['date'])!.value as Date | null;

        return { name: nameInputValue, date: dateInputValue };
    }
}
