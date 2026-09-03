import { CurrencyCodesEnum, WalletTypesEnum } from '../../shared/enums';

// Store-ban használt interface, store-ban nem akarjuk tárolni a wallet összegét
export interface WalletDataInterfaceWithoutSum {
    id: number;
    name: string;
    currencyCode: CurrencyCodesEnum;
    type: WalletTypesEnum;
}

export interface WalletDataInterface extends WalletDataInterfaceWithoutSum {
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
