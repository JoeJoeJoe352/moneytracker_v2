export interface UserData {
    username: string;
    wallets: WalletDataInterface[];
}

export interface WalletDataInterface {
    id: number;
    name: string;
    type: string;
    currencyCode: string;
}
