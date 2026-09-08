import { WritableSignal } from '@angular/core';
import { WalletDataInterface } from '../wallet/interfaces';

export interface UserData {
    username: string;
    wallets: WalletDataInterface[];
}

export interface LoginRequestData {
    username: string;
    password: string;
}
export interface RegisterRequestData {
    username: string;
    email: string;
    password: string;
}

export interface isLoadingInterface {
    isloading: WritableSignal<boolean>;
}
