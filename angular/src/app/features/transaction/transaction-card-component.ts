import { Component, input } from "@angular/core";
import { Transaction } from "./interfaces";
import { TransactionTypeEnum } from "./transaction-type-enum";

@Component({
    selector: "app-transaction-card",
    templateUrl: "./transaction-card.html",
    styleUrl: "./transaction-card.scss",
    imports: []
})
export default class TransactionCardComponent {
    transaction = input.required<Transaction>()

    /**
     * Tranzakció típusa bevétel-e
     */
    isIncome(): boolean {
        return this.transaction().transactionType == TransactionTypeEnum.INCOME
    }
}