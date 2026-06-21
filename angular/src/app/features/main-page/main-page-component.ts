import { Component, signal } from "@angular/core";
import { RegisterModalComponent } from "../transaction/create-transaction-modal";

@Component({
    selector: "app-main-page-component",
    templateUrl: "./main-page-component.html",
    standalone: true,
    imports: [RegisterModalComponent],
})
export class MainPage {
    /**
     * Tranzakció létrehozó modal nyitva van-e
     */
    protected isNewTransactionModalOpen = signal(false);

    /**
     * Tranzakció létrehozó modal felnyitása
     */
    protected openTransactionModal(): void {
        this.isNewTransactionModalOpen.set(true);
    }

    /**
     * Tranzakció létrehozó modal becsukása
     */
    protected closeTransactionModal(): void {
        this.isNewTransactionModalOpen.set(false);
    }
}