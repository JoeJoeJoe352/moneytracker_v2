import { Component, EventEmitter, Input, Output, Signal } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { DropdownInterface } from '../../shared/interfaces';
import { CategoryResponseInterface, DetailForm } from '../transaction/interfaces';
import { CategorySelectComponent } from './category-select-component';
import { Observable } from 'rxjs';
import { MatCardModule } from '@angular/material/card';

@Component({
    selector: 'app-transaction-detail-form-component',
    templateUrl: './transaction-detail-form-component.html',
    styleUrl: './transaction-detail-form-component.scss',
    imports: [
        ReactiveFormsModule,
        TranslatePipe,
        MatSlideToggleModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatIconModule,
        CategorySelectComponent,
        MatCardModule,
    ],
})
export class TransactionDetailFormComponent {
    /**
     * A sorhoz tartozó FormGroup (a szülő details FormArray-jének egy eleme)
     */
    @Input({ required: true }) detail!: FormGroup<DetailForm>;
    /**
     * A sor indexe a details FormArray-ben
     */
    @Input({ required: true }) index!: number;
    /**
     * Kategória adatok a dropdown számára
     */
    @Input({ required: true }) categoryData!: Signal<DropdownInterface[]>;
    /**
     * Kategória mentése folyamatban van-e
     */
    @Input({ required: true }) isCategorySaveInProgress!: boolean;
    /**
     * Ez az utolsó detail sor-e (törlés gomb letiltásához)
     */
    @Input({ required: true }) isLastDetailRow!: boolean;
    /**
     * A kiválasztott wallet-hez tartozó pénznem szimbóluma (Ft, €, $ stb.)
     */
    @Input({ required: true }) currencySymbol!: string;

    @Input() addCategoryCallback!: (name: string) => Observable<CategoryResponseInterface>;
    /**
     * A sor törlés gombjára kattintott a user
     */
    @Output() rowDeleted = new EventEmitter<void>();
}
