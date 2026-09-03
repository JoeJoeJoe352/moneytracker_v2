import { Injectable, signal, WritableSignal } from '@angular/core';
import { UserData } from '../../features/auth/interfaces';
import { WalletDataInterfaceWithoutSum } from '../../features/wallet/interfaces';

@Injectable({
    providedIn: 'root',
})
export class UserDataStore {
    /**
     * name of the user
     */
    public username = signal('');
    /**
     * Store already loaded?
     */
    public isLoaded = signal(false);

    /**
     * Wallet adatok
     */
    private wallets: WritableSignal<WalletDataInterfaceWithoutSum[] | null> = signal(null);

    /**
     * The user is logged in?
     *
     * @returns
     */
    public isUserLogged(): boolean {
        return this.isLoaded() && this.username() !== '';
    }

    /**
     * Reset data (when logged out)
     */
    public resetData(): void {
        this.username.set('');
        this.wallets.set(null);
        this.isLoaded.set(false);
    }

    /**
     * User betöltése
     */
    public loadUserData(userData: UserData): void {
        this.username.set(userData.username);

        this.wallets.set(userData.wallets);
        this.isLoaded.set(true);
    }

    /**
     * Guard a wallet lekérdezéshez
     */
    public getWallets(): WalletDataInterfaceWithoutSum[] {
        const wallets = this.wallets();
        if (!wallets) {
            throw new Error('Wallets are not loaded');
        }
        return wallets;
    }

    /**
     * Visszaadja az alapértelmezett Walletet
     */
    public getDefaultWallet(): WalletDataInterfaceWithoutSum {
        const firstWallet = this.getWallets()[0];

        if (!firstWallet) {
            throw new Error('No default wallet for user');
        }
        return firstWallet;
    }

    /**
     * Beállítja a wallet adatokat
     */
    public setWallets(wallets: WalletDataInterfaceWithoutSum[]): void {
        this.wallets.set(wallets);
    }
}
