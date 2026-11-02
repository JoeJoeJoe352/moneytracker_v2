export enum WalletTypesEnum {
    default = 'DEFAULT',
    savings = 'SAVINGS',
}

export enum CurrencyCodesEnum {
    huf = 'HUF',
    eur = 'EUR',
    usd = 'USD',
}

export enum SupportedLangEnum {
    hu = 'hu',
    en = 'en',
    de = 'de',
}

export enum TransactionTypeEnum {
    INCOME = 'INCOME',
    OUTCOME = 'OUTCOME',
}

export enum TransactionSpecialTypeEnum {
    SYNC = 'SYNC',
    RECURRING = 'RECURRING',
}

export enum RecurrenceIntervalEnum {
    WEEKLY = 'WEEKLY',
    MONTHLY = 'MONTHLY',
}

export enum RecurringTransactionStatusEnum {
    ACTIVE = 'ACTIVE',
    STOPPED = 'STOPPED',
    DELETED = 'DELETED',
}
