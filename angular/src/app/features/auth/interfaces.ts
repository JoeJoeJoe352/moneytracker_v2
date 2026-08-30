import { CurrencyCodes, WalletTypes } from "../../shared/enums";

export interface UserData {
    username: string;
    wallets: WalletDataInterface[];
}

export interface WalletDataInterface {
    id: number;
    name: string;
    type: WalletTypes;
    currencyCode: CurrencyCodes;
}
