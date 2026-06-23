import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { GeneralResponse } from '../auth/auth-service';
import { TransactionTypeEnum } from './transaction-type-enum';
import { newTransaction, Transaction } from './interfaces';

@Injectable({
  providedIn: 'root',
})
export class TransactionService {
  private http = inject(HttpClient);
  // Ez egy globális event, amire fel lehet íratkozni. Nincs értéke, csak az esemény számít
  private refresh$ = new Subject<void>();
  public refreshNeeded$ = this.refresh$.asObservable();

  notifyRefresh() {
    // így lehet egy ilyen eseményt elindítani
    this.refresh$.next();
  }

  /**
   * Tranzakció elmentése
   */
  saveTransaction(transactionData: newTransaction): Observable<GeneralResponse> {
    const { isIncome, price, ...rest } = transactionData;

	  // átalakítjuk az isIncome mező értékét a backend enum-jára
    // kavarodások elkerülése végett nem lehet negatív értéket beírni az árba
    // a kiadás kapcsoló alapján itt állítjuk át negatív értékre, hogyha kell
    const payload = {
      ...rest,
      price: isIncome ? price : price * -1,
      transactionType: isIncome ? TransactionTypeEnum.INCOME : TransactionTypeEnum.OUTCOME,
    };

    return this.http.post<GeneralResponse>('/api/transaction/create', payload, {
      withCredentials: true,
    });
  }

  /**
   * Utolsó X darab tranzakciót lekéri
   */
  getLastTransactions(): Observable<Transaction[]> {
    return this.http.get<Transaction[]>('/api/transaction/last', {
      withCredentials: true,
    })
  }

  /**
   * Utolsó X darab tranzakciót lekéri
   */
  getMoneySum(): Observable<number> {
    return this.http.get<number>('/api/transaction/sum', {
      withCredentials: true,
    })
  }
}
