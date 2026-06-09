import { Component, EventEmitter, Output } from "@angular/core";
import { BaseModal } from "../../shared/components/modal/base-modal";
import { RegisterComponent } from "./register-component";

@Component({
    selector: "app-register-modal",
    template: `
        <app-base-modal [title]="'Registration'" (closeModal)="closeModal.emit()">
            <app-register-component (closeModal)="closeModal.emit()" />
        </app-base-modal>
    `,
    imports: [BaseModal, RegisterComponent],
})
export class RegisterModalComponent {
    @Output() closeModal = new EventEmitter<void>();
}