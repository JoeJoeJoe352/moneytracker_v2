import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { TransactionTypeEnum } from './transaction-type-enum';
import { NewTransaction, TransactionDataForBackend, TransactionDataFromBackend, TransactionInputDefaultValues } from './interfaces';

@Injectable({
  providedIn: 'root',
})
export class TransactionService {
  private readonly http = inject(HttpClient);

  /**
   * Tranzakció létrehozása
   */
  saveTransaction(transactionData: NewTransaction): Observable<void> {
    const payload = this.convertToBackendData(transactionData)

    return this.http.post<void>('/api/transaction', payload);
  }

    /**
   * Tranzakció frissítése
   */
  updateTransaction(transactionData: NewTransaction, id: number): Observable<void> {
    const payload = this.convertToBackendData(transactionData)

    return this.http.put<void>('/api/transaction/' + id, payload);
  }

  /**
   * Utolsó X darab tranzakciót lekéri
   */
  getLastTransactions(): Observable<TransactionDataFromBackend[]> {
    return this.http.get<TransactionDataFromBackend[]>('/api/transaction/last')
  }

  /**
   * History lekérdezése, paraméterekkel
   * 
   * @param params 
   * @returns 
   */
  getTransactionHistory(params: URLSearchParams): Observable<TransactionDataFromBackend[]> {
    const paramString = params.toString()

    return this.http.get<TransactionDataFromBackend[]>('/api/transaction/history?' + paramString)
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
  getTransactionById(transactionId: number): Observable<TransactionDataFromBackend> {
    return this.http.get<TransactionDataFromBackend>('/api/transaction/' + transactionId)
  }

  /**
   * Átalakítja a tranzakciós adat modellt arra formára, amit az inputok elvárnak
   */
  convertDataToInput(transaction: TransactionDataFromBackend): TransactionInputDefaultValues {
    const isIncome = transaction.transactionType === TransactionTypeEnum.INCOME;
    const transactionDate = new Date(transaction.transactionDate);

    return {
      name: transaction.name,
      // Backend negatívba tárolja a kiadást.
      // Ha abszolut értéket vennék, az eltüntethet potenciális hibát
      price: isIncome ? transaction.priceSum : transaction.priceSum * -1,
      isIncome: isIncome,
      transactionDate: transactionDate,
    }
  }

  /**
   * A Tranzakciós form inputból kapott adatokat átalakítja a backend számára megfelelő formába
   */
  convertToBackendData(input: NewTransaction): TransactionDataForBackend {
    const { transactionDate, isIncome, price, ...rest } = input;

    const transactionDateString = transactionDate.toISOString().slice(0, 10)

	  // átalakítjuk az isIncome mező értékét a backend enum-jára
    const TransactionyTypeString = isIncome ? TransactionTypeEnum.INCOME : TransactionTypeEnum.OUTCOME

    // kavarodások elkerülése végett nem lehet negatív értéket beírni az árba a kiadás kapcsoló alapján itt állítjuk át negatív értékre, hogyha kell
    const priceFormatted = isIncome ? price : price * -1;

    return {
      name: rest.name,
      price: priceFormatted,
      transactionType: TransactionyTypeString,
      transactionDate: transactionDateString,
    };
  }
}
