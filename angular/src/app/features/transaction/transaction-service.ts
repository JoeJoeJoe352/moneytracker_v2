import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { GeneralResponse } from "../auth/auth-service";

interface newTransaction {
	name: string,
	transactionTypeBool: boolean,
	price: number,
	transactionDate: string,
};

@Injectable({
  providedIn: 'root',
})
export class TransactionService {
  private http = inject(HttpClient)

  saveTransaction(transactionData: newTransaction): Observable<GeneralResponse> {
			const { transactionTypeBool, ...rest } = transactionData;
			
			const payload = {
			  ...rest,
			  transactionType: transactionTypeBool ? 'INCOME' : 'OUTCOME',
			};


    return this.http.post<GeneralResponse>(
      '/api/transaction/create', 
      payload,
			{ withCredentials: true }
    );
  }

}