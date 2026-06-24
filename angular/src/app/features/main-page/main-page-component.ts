import { Component, signal } from "@angular/core";
import MoneySumComponent from "../transaction/money-sum-component";
import TransactionListComponent from "../transaction/transaction-list-component";
import { TransactionModalComponent } from "../transaction/create-transaction-modal";

@Component({
    selector: "app-main-page-component",
    templateUrl: "./main-page-component.html",
    styleUrl: "./main-page-component.scss",
    standalone: true,
    imports: [TransactionModalComponent, MoneySumComponent, TransactionListComponent],
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