import { Component, signal } from "@angular/core";
import { RegisterModalComponent } from "../transaction/create-transaction-modal";

@Component({
    selector: "app-main-page-component",
    template: `
        <div>main page</div>
        <p>
            <button class="btn btn-secondary" (click)="openModal()">Create new transaction</button>
        </p>
        
        @if (isModalOpen()) {
            <app-create-transaction-modal (closeModal)="closeModal()" />
        }
    `,
    standalone: true,
    imports: [RegisterModalComponent],
})
export class MainPage {
    isModalOpen = signal(false);

    openModal() {
        this.isModalOpen.set(true);
    }

    closeModal() {
        this.isModalOpen.set(false);
    }
}