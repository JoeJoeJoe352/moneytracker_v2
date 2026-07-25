import { inject, Injectable } from "@angular/core";
import { TransactionService } from "./transaction-service";
import { TranslateService } from "@ngx-translate/core";

@Injectable()
export class TransactionFacade {
    private transactionService = inject(TransactionService)
    private translateService = inject(TranslateService)


}