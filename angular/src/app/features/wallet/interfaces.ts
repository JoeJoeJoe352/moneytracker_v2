import { CurrencyCodesEnum, WalletTypesEnum } from "../../shared/enums";

export interface WalletDataInterface {
    id: number;
    name: string;
    currencyCode: CurrencyCodesEnum;
    type: WalletTypesEnum;
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
