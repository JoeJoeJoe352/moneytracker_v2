import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { GeneralResponse } from '../auth/auth-service';
import { TransactionTypeEnum } from './transaction-type-enum';

interface newTransaction {
  name: string;
  isIncome: boolean;
  price: number;
  transactionDate: string;
}

@Injectable({
  providedIn: 'root',
})
export class TransactionService {
  private http = inject(HttpClient);

  /**
   * Tranzakció elmentése
   */
  saveTransaction(transactionData: newTransaction): Observable<GeneralResponse> {
    const { isIncome, ...rest } = transactionData;

	// átalakítjuk az isIncome mező értékét a backend enum-jára
    const payload = {
      ...rest,
      transactionType: isIncome ? TransactionTypeEnum.INCOME : TransactionTypeEnum.OUTCOME,
    };

    return this.http.post<GeneralResponse>('/api/transaction/create', payload, {
      withCredentials: true,
    });
  }
}
