import { Component, input, output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { DropdownInterface } from '../../../shared/interfaces';
import { CategoryResponseInterface, DetailForm } from '../interfaces';
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
    public detail = input.required<FormGroup<DetailForm>>();
    /**
     * A sor indexe a details FormArray-ben
     */
    public index = input.required<number>();
    /**
     * Kategória adatok a dropdown számára
     */
    public categoryData = input.required<DropdownInterface[]>();
    /**
     * Kategória mentése folyamatban van-e
     */
    public isCategorySaveInProgress = input.required<boolean>();
    /**
     * Ez az utolsó detail sor-e (törlés gomb letiltásához)
     */
    public isLastDetailRow = input.required<boolean>();
    /**
     * A kiválasztott wallet-hez tartozó pénznem szimbóluma (Ft, €, $ stb.)
     */
    public currencySymbol = input.required<string>();
    /**
     * Kategória hozzáadásakor lefutó callback
     */
    public addCategoryCallback =
        input.required<(name: string) => Observable<CategoryResponseInterface>>();
    /**
     * A sor törlés gombjára kattintott a user
     */
    public rowDeleted = output<void>();
}
