import { WalletDataInterface } from '../wallet/interfaces';

export interface UserData {
    username: string;
    wallets: WalletDataInterface[];
}

export interface LoginRequestData {
    username: string;
    password: string;
}
