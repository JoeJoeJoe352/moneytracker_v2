/**
 * Tranzakciós adatok, ahogy a form inpujai elvárja kezdőértéknek
 */
export interface TransactionInput {
  name: string,
  isIncome: boolean,
  price: null|number, // kezdőértéknek null a price
  transactionDate: Date,
}

/**
 * Tranzakciós adatok, ahogy a form kitöltése után lesznek
 */
export interface newTransaction {
  name: string;
  isIncome: boolean;
  price: number;
  transactionDate: string;
}

/**
 * Tranzakció adatai a backendről
 */
export interface Transaction {
  id: number,
  name: string,
  priceSum: number,
  transactionDate: string,
  transactionType: string,
}

