import { Signal } from '@angular/core';
import { CurrencyCodesEnum, WalletTypesEnum } from '@shared/enums';

export interface WalletDataInterface {
    id: number;
    name: string;
    currencyCode: CurrencyCodesEnum;
    type: WalletTypesEnum;
    lastSyncDate: number;
    sum: number;
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
    wallet: WalletDataInterface;
    isLoading: Signal<boolean>;
}

export interface WalletSyncData {
    walletId: number;
    currentBalance: number;
}

export interface WalletSyncDataToBackend extends WalletSyncData {
    // user dátuma, az időzónában, ez a user időzónájától függ
    currentDate: string;
}
