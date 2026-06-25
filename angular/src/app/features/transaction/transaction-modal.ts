import { Component, EventEmitter, Input, Output } from "@angular/core";
import { BaseModal } from "../../shared/components/modal/base-modal";
import { TransactionFormComponent } from "./transaction-form-component";
import { Transaction } from "./interfaces";

@Component({
    selector: "app-create-transaction-modal",
    template: `
        <app-base-modal [title]="'Create transaction'" (closeModal)="closeModal.emit()">
            <app-transaction-form-component
                (closeModal)="closeModal.emit()"
                (dataChanged)="dataChanged.emit()"
                [transaction]="transaction"
             />
        </app-base-modal>
    `,
    imports: [BaseModal, TransactionFormComponent],
})
export class TransactionModalComponent {
    @Input() transaction: Transaction | null = null;
    
    @Output() closeModal = new EventEmitter<void>();
    @Output() dataChanged = new EventEmitter<void>();
}