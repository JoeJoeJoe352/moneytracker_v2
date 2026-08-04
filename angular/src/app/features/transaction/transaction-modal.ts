import { Component, EventEmitter, Input, Output } from '@angular/core';
import { BaseModal } from '../../shared/components/modal/base-modal';
import { TransactionFormComponent } from './transaction-form-component';
import { NewTransaction, TransactionDataFromBackend } from './interfaces';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'app-create-transaction-modal',
    template: `
        <app-base-modal [title]=" 'transaction.create' | translate " (closeModal)="closeModal.emit()">
            <app-transaction-form-component
                [isTransactionFormDisabled]="isTransactionFormDisabled"
                [transaction]="transaction"
                (deleted)="deleteTransactionRequested.emit($event)"
                (saved)="saved.emit($event)"
            />
        </app-base-modal>
    `,
    imports: [BaseModal, TransactionFormComponent, TranslatePipe],
})
export class TransactionModalComponent {
    @Input() transaction: TransactionDataFromBackend | null = null;
    @Input({ required: true }) isTransactionFormDisabled!: boolean;

    @Output() closeModal = new EventEmitter<void>();
    @Output() deleteTransactionRequested = new EventEmitter<number>();
    @Output() saved = new EventEmitter<NewTransaction>();
}
