/**
 * Tranzakciós adatok, ahogy a form inpujai elvárja kezdőértéknek
 */
export interface TransactionInputDefaultValues {
  name: string,
  isIncome: boolean,
  price: null|number,
  transactionDate: Date,
}

/**
 * Tranzakciós adatok, ahogy a form kitöltése után lesznek
 * Kezdőértéknek lehet null a price, de elküldés után már nem
 */
export interface NewTransaction extends Omit<TransactionInputDefaultValues, 'price'> {
  price: number;
}

/**
 * Tranzakciós adatok, abban a formában, ahogy a backend feldolgozná
 */
export interface TransactionDataForBackend {
  name: string;
  price: number;
  transactionDate: string;
  transactionType: string;
}

/**
 * Tranzakció adatai, abban a formában, ahogyan a backendről jön
 */
export interface TransactionDataFromBackend {
  id: number,
  name: string,
  priceSum: number,
  transactionDate: string,
  transactionType: string,
}

