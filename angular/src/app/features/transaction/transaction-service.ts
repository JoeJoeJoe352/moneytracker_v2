import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { TransactionTypeEnum } from './transaction-type-enum';
import { newTransaction, Transaction, TransactionInput } from './interfaces';

@Injectable({
  providedIn: 'root',
})
export class TransactionService {
  private readonly http = inject(HttpClient);

  /**
   * Tranzakció elmentése. Létrehoz, vagy frissít
   */
  saveTransaction(transactionData: newTransaction, id: number|null): Observable<void> {
    const { isIncome, price, ...rest } = transactionData;

	  // átalakítjuk az isIncome mező értékét a backend enum-jára
    // kavarodások elkerülése végett nem lehet negatív értéket beírni az árba
    // a kiadás kapcsoló alapján itt állítjuk át negatív értékre, hogyha kell
    const payload = {
      ...rest,
      price: isIncome ? price : price * -1,
      transactionType: isIncome ? TransactionTypeEnum.INCOME : TransactionTypeEnum.OUTCOME,
    };

    if (id === null) {
      // Tranzakció létrehozása
      return this.http.post<void>('/api/transaction', payload);
    } else {
      // Meglévő tranzakció frissítése
      return this.http.put<void>('/api/transaction/' + id, payload);
    }
  }

  /**
   * Utolsó X darab tranzakciót lekéri
   */
  getLastTransactions(): Observable<Transaction[]> {
    return this.http.get<Transaction[]>('/api/transaction/last')
  }

  /**
   * History lekérdezése, paraméterekkel
   * 
   * @param params 
   * @returns 
   */
  getTransactionHistory(params: URLSearchParams): Observable<Transaction[]> {
    const paramString = params.toString()
    return this.http.get<Transaction[]>('/api/transaction/history?' + paramString)
  }

  /**
   * Utolsó X darab tranzakciót lekéri
   */
  getMoneySum(): Observable<number> {
    return this.http.get<number>('/api/transaction/sum')
  }

  /**
   * Lekéri a megadott azonosítójú tranzakciót
   */
  getTransactionById(transactionId: number): Observable<Transaction> {
    return this.http.get<Transaction>('/api/transaction/' + transactionId)
  }

  /**
   * Átalakítja a tranzakciós adat modellt arra  formára, amit az inputok elvárnak
   */
  convertDataToInput(transaction: Transaction): TransactionInput {
    const isIncome = transaction.transactionType === TransactionTypeEnum.INCOME;
    const transactionDate = new Date(transaction.transactionDate);

    return {
      name: transaction.name,
      // backend negatívba tárolja a kiadást
      price: isIncome ? transaction.priceSum : transaction.priceSum * -1,
      isIncome: isIncome,
      transactionDate: transactionDate,
    }
  }
}
