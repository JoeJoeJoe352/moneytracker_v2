import { Component } from "@angular/core";
import { TransactionFormComponent } from "../transaction/transaction-form-component";

@Component({
    selector: "app-main-page-component",
    template: `
        <div>main page</div>
        <app-transaction-form-component />
    `,
    standalone: true,
    imports: [TransactionFormComponent],
})
export class MainPage {

}