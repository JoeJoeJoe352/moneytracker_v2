import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { TimeHelper } from '@shared/services/time-helper';
import { TransferTransactionCreateRequest, TransferTransactionFormValue } from './interfaces';

@Injectable({
    providedIn: 'root',
})
export class TransferTransactionService {
    private readonly http = inject(HttpClient);
    private readonly timeHelper = inject(TimeHelper);

    /**
     * Átvezetés létrehozása két wallet között
     */
    public createTransferTransaction(formValue: TransferTransactionFormValue): Observable<void> {
        const payload: TransferTransactionCreateRequest = {
            srcTransactionName: formValue.srcTransactionName,
            srcWalletId: formValue.srcWalletId,
            dstTransactionName: formValue.dstTransactionName,
            dstWalletId: formValue.dstWalletId,
            transactionDate: this.timeHelper.formatDateToString(formValue.transactionDate),
            priceSum: formValue.priceSum,
        };

        return this.http.post<void>('/api/transfer-transaction', payload);
    }
}
