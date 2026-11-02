import {
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
