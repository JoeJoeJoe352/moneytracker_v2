import { Component, input } from "@angular/core";
import { Transaction } from "./interfaces";

@Component({
    selector: "app-transaction-card",
    templateUrl: "./transaction-card.html",
    styleUrl: "./transaction-card.scss"
})
export default class TransactionCardComponent {
    transaction = input.required<Transaction>()
}