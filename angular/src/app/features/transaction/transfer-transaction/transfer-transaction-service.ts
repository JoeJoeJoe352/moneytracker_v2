import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { TimeHelper } from '@shared/services/time-helper';
import {
    TransferTransactionCreateRequest,
    TransferTransactionFormValue,
    TransferTransactionResponse,
    TransferTransactionUpdateRequest,
} from './interfaces';

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

    /**
     * Lekéri az átvezetést, aminek a megadott tranzakció az egyik oldala
     */
    public getTransferTransactionByTransactionId(
        transactionId: number,
    ): Observable<TransferTransactionResponse> {
        return this.http.get<TransferTransactionResponse>(
            '/api/transfer-transaction/by-transaction/' + transactionId,
        );
    }

    /**
     * Átvezetés módosítása. A walletek nem módosíthatók, ezért nem is küldjük őket
     */
    public updateTransferTransaction(
        transferId: number,
        formValue: TransferTransactionFormValue,
    ): Observable<void> {
        const payload: TransferTransactionUpdateRequest = {
            srcTransactionName: formValue.srcTransactionName,
            dstTransactionName: formValue.dstTransactionName,
            transactionDate: this.timeHelper.formatDateToString(formValue.transactionDate),
            priceSum: formValue.priceSum,
        };

        return this.http.put<void>('/api/transfer-transaction/' + transferId, payload);
    }

    /**
     * Átvezetés törlése (mindkét oldala)
     */
    public deleteTransferTransaction(transferId: number): Observable<void> {
        return this.http.delete<void>('/api/transfer-transaction/' + transferId);
    }
}
