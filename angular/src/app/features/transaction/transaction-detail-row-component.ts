import { Component, EventEmitter, Input, Output, Signal } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { IDropdownSettings, NgMultiSelectDropDownModule } from 'ng-multiselect-dropdown';
import { SwitchComponent } from '../../shared/components/switch.component';
import { DropdownInterface } from '../../shared/interfaces';
import { DetailForm } from './interfaces';

@Component({
    selector: 'app-transaction-detail-row-component',
    templateUrl: './transaction-detail-row-component.html',
    styleUrls: ['./transaction-detail-row-component.scss'],
    imports: [ReactiveFormsModule, TranslatePipe, NgMultiSelectDropDownModule, SwitchComponent],
})
export class TransactionDetailRowComponent {
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
     * MultiselectSettings beállításai
     */
    @Input({ required: true }) multiselectSettings!: Signal<IDropdownSettings>;
    /**
     * Kategória mentése folyamatban van-e
     */
    @Input({ required: true }) isCategorySaveInProgress!: boolean;
    /**
     * Ez az utolsó detail sor-e (törlés gomb letiltásához)
     */
    @Input({ required: true }) isLastDetailRow!: boolean;

    /**
     * A kategória dropdown keresőmezőjének szövege változott
     */
    @Output() categoryFilterChange = new EventEmitter<unknown>();
    /**
     * A kategória dropdown-ra kattintott a user
     */
    @Output() categoryDropdownClick = new EventEmitter<Event>();
    /**
     * A kategória dropdown bezáródott, a keresőszöveget törölni kell
     */
    @Output() categorySearchTextCleared = new EventEmitter<void>();
    /**
     * A sor törlés gombjára kattintott a user
     */
    @Output() rowDeleted = new EventEmitter<void>();
}
