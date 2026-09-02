import { WalletDataInterface } from "../wallet/interfaces";

export interface UserData {
    username: string;
    wallets: WalletDataInterface[];
}
