import { computed } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { UserData } from '../../features/auth/interfaces';
import { WalletDataInterfaceWithoutSum } from '../../features/wallet/interfaces';

interface UserDataState {
    /**
     * Felhasználónév
     */
    _username: string;
    /**
     * Store adatai be vannak-e már töltve
     */
    _isLoaded: boolean;
    /**
     * Wallet adatok
     */
    _wallets: WalletDataInterfaceWithoutSum[] | null;
}

const initialState: UserDataState = {
    _username: '',
    _isLoaded: false,
    _wallets: null,
};

export const UserDataStore = signalStore(
    { providedIn: 'root' },
    withState(initialState),
    withComputed(({ _username, _isLoaded }) => ({
        /**
         * The user is logged in?
         */
        isUserLogged: computed(() => _isLoaded() && _username() !== ''),
    })),
    withMethods((store) => ({
        /**
         * Guard a wallet lekérdezéshez
         * Külön withMethods, hogy a getDefaultWallet tudjon rá hivatkozni
         */
        getWallets(): WalletDataInterfaceWithoutSum[] {
            const wallets = store._wallets();
            if (!wallets) {
                throw new Error('Wallets are not loaded');
            }
            return wallets;
        },
    })),
    withMethods((store) => ({
        /**
         * Reset data (when logged out)
         */
        resetData(): void {
            patchState(store, initialState);
        },

        /**
         * User betöltése
         */
        loadUserData(userData: UserData): void {
            patchState(store, {
                _username: userData.username,
                _wallets: userData.wallets,
                _isLoaded: true,
            });
        },

        /**
         * Visszaadja az alapértelmezett Walletet
         */
        getDefaultWallet(): WalletDataInterfaceWithoutSum {
            const firstWallet = store.getWallets()[0];

            if (!firstWallet) {
                throw new Error('No default wallet for user');
            }
            return firstWallet;
        },

        /**
         * Beállítja a wallet adatokat
         */
        setWallets(wallets: WalletDataInterfaceWithoutSum[]): void {
            patchState(store, { _wallets: wallets });
        },

        getUsername() {
            return store._username
        }
    })),
);
