import { Component, EventEmitter, Output, signal, WritableSignal } from "@angular/core";
import { baseModal } from "../../../shared/components/modal/base-modal";
import { RegisterComponent } from "./RegisterComponent";

@Component({
    selector: "register-modal",
    template: `
        <base-modal [title]="'Registration'" (close)="close.emit()">
            <register-component (closeModal)="close.emit()" />
        </base-modal>
    `,
    imports: [baseModal, RegisterComponent],
})
export class RegisterModalComponent {
    @Output() close = new EventEmitter<void>();
}