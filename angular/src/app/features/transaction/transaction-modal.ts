import { Component, EventEmitter, inject, Output, Signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';
import {
    CategoryResponseInterface,
    NewTransaction,
    TransactionDataFromBackend,
} from './interfaces';
import { TransactionFormComponent } from '../transaction-form/transaction-form-component';

export interface TransactionModalInputInterface {
    transaction: Signal<TransactionDataFromBackend | null>;
    categories: Signal<CategoryResponseInterface[]>;
    isTransactionFormDisabled: Signal<boolean>;
    isCategorySaveInProgress: Signal<boolean>;
    isDataInitializing: Signal<boolean>;
}

@Component({
    selector: 'app-create-transaction-modal',
    templateUrl: './transaction-modal.html',
    styleUrl: './transaction-modal.scss',
    standalone: true,
    imports: [
        TransactionFormComponent,
        TranslatePipe,
        MatDialogModule,
        MatProgressSpinnerModule,
        MatIconModule,
        MatButtonModule,
    ],
})
export class TransactionModalComponent {
    public data = inject<TransactionModalInputInterface>(MAT_DIALOG_DATA);

    @Output() deleteTransactionRequested = new EventEmitter<number>();
    @Output() saved = new EventEmitter<NewTransaction>();
    @Output() categoryAdded = new EventEmitter<string>();
}
