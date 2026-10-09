import { Signal } from '@angular/core';
import { CurrencyCodesEnum, WalletTypesEnum } from '@shared/enums';

export interface WalletDataInterface {
    id: number;
    name: string;
    currencyCode: CurrencyCodesEnum;
    type: WalletTypesEnum;
    lastSyncDate: string;
     // note: ez lehet elavult tranzakció változás esetén. 
     // Akkor használható csak biztonsággal, ha előtte valami betölti a garantáltan frisset
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

export interface WalletSyncDataToBackend {
    currentBalance: number;
    syncDate: string;
}
