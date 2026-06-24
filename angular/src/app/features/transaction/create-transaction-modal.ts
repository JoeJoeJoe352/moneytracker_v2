import { Component, EventEmitter, Output } from "@angular/core";
import { BaseModal } from "../../shared/components/modal/base-modal";
import { TransactionFormComponent } from "./transaction-form-component";

@Component({
    selector: "app-create-transaction-modal",
    template: `
        <app-base-modal [title]="'Create transaction'" (closeModal)="closeModal.emit()">
            <app-transaction-form-component (closeModal)="closeModal.emit()" />
        </app-base-modal>
    `,
    imports: [BaseModal, TransactionFormComponent],
})
export class TransactionModalComponent {
    @Output() closeModal = new EventEmitter<void>();
}