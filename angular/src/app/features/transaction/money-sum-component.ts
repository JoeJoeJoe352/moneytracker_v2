import { Component, inject, OnInit, signal } from "@angular/core";
import { TransactionService } from "./transaction-service";
import { DecimalPipe } from "@angular/common";

@Component({
    selector: "app-money-sum",
    template: `
        {{ moneySum() | number }} Ft
    `,
    imports: [DecimalPipe],
})
export default class MoneySumComponent implements OnInit {
    transactionService = inject(TransactionService)
    moneySum = signal(0)
    isLoaded = signal(false)

    ngOnInit() {
        this.transactionService.getMoneySum().subscribe({
            next: (response) => {
                this.isLoaded.set(true);
                this.moneySum.set(response)
            },
            error: (response) => {
                console.error("unknown error during transaction creation!", response);
                this.isLoaded.set(false);
            },
        })
    }
}