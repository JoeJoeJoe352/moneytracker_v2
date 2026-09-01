import { Injectable, signal, WritableSignal } from '@angular/core';
import { UserData } from '../../features/auth/interfaces';
import { WalletDataInterface } from '../../features/wallet/interfaces';

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
    private wallets: WritableSignal<WalletDataInterface[] | null> = signal(null);

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
    public getWallets(): WalletDataInterface[] {
      const wallets = this.wallets();
      if (!wallets) {
        throw new Error("Wallets are not loaded")
      }
      return wallets;
    }

    /**
     * Visszaadja az alapértelmezett Walletet
     */
    public getDefaultWallet(): WalletDataInterface {
      const firstWallet = this.getWallets().pop()

      if (!firstWallet) {
        throw new Error("No default wallet for user");
      }
      return firstWallet
    }
}
