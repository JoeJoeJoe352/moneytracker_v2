export interface newTransaction {
  name: string;
  isIncome: boolean;
  price: number;
  transactionDate: string;
}

export interface Transaction {
  id: number,
  name: string,
  priceSum: number,
  transactionDate: string,
  transactionType: string,
}