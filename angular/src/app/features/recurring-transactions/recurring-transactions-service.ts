import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import {
    RecurringTransactionResponseInterface,
    RecurringTransactionSaveRequestInterface,
} from './interfaces';

@Service()
export class RecurringTransactionService {
    private readonly http = inject(HttpClient);

    /**
     * Visszatér a felhasználó ismétlődő tranzakcióival
     */
    listRecurringTransactions(): Observable<RecurringTransactionResponseInterface[]> {
        return this.http.get<RecurringTransactionResponseInterface[]>('/api/recurring-transaction');
    }

    /**
     * Ismétlődő tranzakció létrehozása
     */
    createRecurringTransaction(data: RecurringTransactionSaveRequestInterface): Observable<void> {
        return this.http.post<void>('/api/recurring-transaction', data);
    }

    /**
     * Megadott id-jú ismétlődő tranzakció módosítása
     */
    updateRecurringTransaction(
        id: number,
        data: RecurringTransactionSaveRequestInterface,
    ): Observable<void> {
        return this.http.put<void>(`/api/recurring-transaction/${id}`, data);
    }

    /**
     * Megadott id-jú ismétlődő tranzakció leállítása
     */
    stopRecurringTransaction(id: number): Observable<void> {
        return this.http.put<void>(`/api/recurring-transaction/${id}/stop`, null);
    }

    /**
     * Megadott id-jú ismétlődő tranzakció törlése
     */
    deleteRecurringTransaction(id: number): Observable<void> {
        return this.http.delete<void>(`/api/recurring-transaction/${id}`);
    }
}
