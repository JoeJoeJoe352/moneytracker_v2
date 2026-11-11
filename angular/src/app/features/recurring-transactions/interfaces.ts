import {
    CurrencyCodesEnum,
    RecurrenceIntervalEnum,
    RecurringTransactionStatusEnum,
    TransactionTypeEnum,
} from '@app/shared/enums';
import { CategoryResponseInterface } from '../transaction/interfaces';
import { WalletDataInterface } from '../wallet/interfaces';

export interface RecurringTransactionResponseInterface {
    id: number;
    name: string;
    priceSum: number;
    startDate: string;
    nextOccurrenceDate: string;
    recurrenceInterval: RecurrenceIntervalEnum;
    transactionType: TransactionTypeEnum;
    status: RecurringTransactionStatusEnum;
    categories: CategoryResponseInterface[];
    wallet: WalletDataInterface;
}

export interface RecurringTransactionSaveRequestInterface {
    name: string;
    priceSum: number;
    /**
     * Létrehozáskor a kezdő dátum, módosításkor a következő esedékesség dátuma (Y-m-d)
     */
    startDate: string;
    recurrenceInterval: RecurrenceIntervalEnum;
    transactionType: TransactionTypeEnum;
    categoryIds: number[];
    walletId: number;
    /**
     * Leállított ismétlődő tranzakció újraindítása
     */
    isRestart: boolean;
}

export interface MonthlyStatInterface {
    recurringTransactionId: number;
    name: string;
    recurringTransactionPrice: number;
    nextOccurrenceDate: string;
    transactionType: TransactionTypeEnum;
    createdSum: number;
    plannedSum: number;
    currencyCode: CurrencyCodesEnum;
}
