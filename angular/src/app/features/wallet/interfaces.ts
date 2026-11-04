import { Signal } from '@angular/core';
import { CurrencyCodesEnum, WalletTypesEnum } from '@shared/enums';
import type { RecurringTransactionResponseInterface } from '../recurring-transactions/interfaces';

export interface WalletDataInterface {
    id: number;
    name: string;
    currencyCode: CurrencyCodesEnum;
    type: WalletTypesEnum;
}

export interface WalletListDataInterface {
    id: number;
    name: string;
    currencyCode: CurrencyCodesEnum;
    type: WalletTypesEnum;
    lastSyncDate: string;
    // note: ez lehet elavult tranzakció változás esetén.
    // Akkor használható csak biztonsággal, ha előtte valami betölti a garantáltan frisset
    sum: number;
    deletable: boolean;
    /**
     * A wallethez tartozó (nem törölt) ismétlődő tranzakciók, a legközelebb esedékes elöl
     */
    recurringTransactions: RecurringTransactionResponseInterface[];
}

export interface WalletCreateRequest {
    name: string;
    currencyCode: CurrencyCodesEnum;
    walletType: WalletTypesEnum;
}

export interface WalletUpdateRequest {
    name: string;
    walletType: WalletTypesEnum;
}

export interface WalletSyncFormInputInterface {
    wallet: WalletListDataInterface;
    isLoading: Signal<boolean>;
}

export interface WalletSyncData {
    walletId: number;
    currentBalance: number;
}

export interface WalletSyncDataToBackend {
    currentBalance: number;
}
