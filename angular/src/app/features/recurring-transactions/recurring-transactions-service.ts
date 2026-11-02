import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { RecurringTransactionResponseInterface } from './interfaces';

@Service()
export class RecurringTransactionService {
    private readonly http = inject(HttpClient);

    /**
     * Visszatér a felhasználó walletjaival
     */
    listRecurringTransactions(): Observable<RecurringTransactionResponseInterface[]> {
        return this.http.get<RecurringTransactionResponseInterface[]>('/api/recurring-transaction');
    }
}
