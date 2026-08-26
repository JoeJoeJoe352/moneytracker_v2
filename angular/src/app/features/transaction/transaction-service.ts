import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MoneySumInterface, NewTransaction, TransactionDataFromBackend } from './interfaces';
import { TransactionUtils } from './transaction-utils';
import { TransactionListElementData } from '../transaction-list/interfaces';

@Injectable({
  providedIn: 'root',
})
export class TransactionService {
  private readonly http = inject(HttpClient);
  public readonly utils = inject(TransactionUtils)

  /**
   * Tranzakció létrehozása
   */
  saveTransaction(transactionData: NewTransaction): Observable<void> {
    const payload = this.utils.convertToBackendData(transactionData)

    return this.http.post<void>('/api/transaction', payload);
  }

    /**
   * Tranzakció frissítése
   */
  updateTransaction(transactionData: NewTransaction, id: number): Observable<void> {
    const payload = this.utils.convertToBackendData(transactionData)

    return this.http.put<void>('/api/transaction/' + id, payload);
  }

  /**
   * Utolsó X darab tranzakciót lekéri
   */
  getLastTransactions(): Observable<TransactionListElementData[]> {
    return this.http.get<TransactionListElementData[]>('/api/transaction/last')
  }

  /**
   * Adott tranzakció törlése
   */
  deleteTransaction(transactionId: number): Observable<void> {
    return this.http.delete<void>('/api/transaction/' + transactionId)
  }

  /**
   * History lekérdezése, paraméterekkel
   * 
   * @param params 
   * @returns 
   */
  getTransactionHistory(params: URLSearchParams): Observable<TransactionListElementData[]> {
    const paramString = params.toString()

    return this.http.get<TransactionListElementData[]>('/api/transaction/history?' + paramString)
  }

  /**
   * Utolsó X darab tranzakciót lekéri
   */
  getMoneySum(): Observable<MoneySumInterface> {
    return this.http.get<MoneySumInterface>('/api/transaction/sum')
  }

  /**
   * Lekéri a megadott azonosítójú tranzakciót
   */
  getTransactionById(transactionId: number): Observable<TransactionDataFromBackend> {
    return this.http.get<TransactionDataFromBackend>('/api/transaction/' + transactionId)
  }
}
