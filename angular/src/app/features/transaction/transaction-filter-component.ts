import { Component, inject, input, OnInit, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe } from '@ngx-translate/core';
import type { FilterData } from './transactions-component';

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
    public defaultData = input.required<FilterData>();
    /**
     * Tranzakciós adatok töltődnek-e
     */
    public isTransactionListLoading = input.required<boolean>();
    /**
     * A cím heading szintje (aria-level), a szülő komponens heading-hierarchiájától függően
     */
    public headingLevel = input(2);
    /**
     * Form elküldése event
     */
    public filterSubmit = output<FilterData>();
    /**
     * Form resetelése event
     */
    public resetForm = output<void>();

    /**
     * Form definiciója. A kezdőértékeket az ngOnInit tölti bele
     */
    protected readonly filterForm = this.fb.nonNullable.group({
        transactionId: this.fb.control<number | null>(null, [Validators.min(1)]),
        name: [''],
        date: this.fb.control<Date | null>(null),
    });

    ngOnInit(): void {
        this.filterForm.patchValue(this.defaultData());
    }

    /**
     * keresési adatok resetelése. Üres értékekre állít vissza
     */
    protected clearInputs(): void {
        this.filterForm.reset();
        this.resetForm.emit();
    }

    /**
     * Form küldése
     */
    protected submitForm(): void {
        if (!this.filterForm.valid) {
            return;
        }
        this.filterSubmit.emit(this.getValuesFromFilterInputs());
    }

    /**
     * A filter inputok értékei alapján létrehoz egy FilterData objektumot
     */
    private getValuesFromFilterInputs(): FilterData {
        const { transactionId, name, date } = this.filterForm.getRawValue();

        return { transactionId, name: name.trim(), date };
    }
}
