import { computed } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { UserData } from '../../features/auth/interfaces';
import { WalletDataInterfaceWithoutSum } from '../../features/wallet/interfaces';

interface UserDataState {
    /**
     * Felhasználó azonosítója (privát)
     */
    _id: number | null;
    /**
     * Felhasználónév (privát)
     */
    _username: string;
    /**
     * Store adatai be vannak-e már töltve (privát)
     */
    _isLoaded: boolean;
    /**
     * Wallet adatok (privát)
     */
    _wallets: WalletDataInterfaceWithoutSum[] | null;
}

const initialState: UserDataState = {
    _id: null,
    _username: '',
    _isLoaded: false,
    _wallets: null,
};

export const UserDataStore = signalStore(
    { providedIn: 'root' },
    withState(initialState),
    withComputed(({ _username, _isLoaded }) => ({
        /**
         * Be van-e jelentkezve a user
         */
        isUserLogged: computed(() => _isLoaded() && _username() !== ''),
    })),
    withMethods((store) => ({
        /**
         * Walletek lekérése
         * Külön withMethods-ban található, hogy a második blokkban lévő függvények meg tudják hívni őket
         */
        getWallets(): WalletDataInterfaceWithoutSum[] {
            const wallets = store._wallets();
            if (!wallets) {
                throw new Error('Wallets are not loaded');
            }
            return wallets;
        },
        /**
         * Beállítja a wallet adatokat
         */
        setWallets(wallets: WalletDataInterfaceWithoutSum[]): void {
            patchState(store, { _wallets: wallets });
        },
    })),
    withMethods((store) => ({
        /**
         * Adatok alaphelyzetbe állítása (kijelentkezéskor)
         */
        resetData(): void {
            patchState(store, initialState);
        },

        /**
         * User betöltése
         */
        loadUserData(userData: UserData): void {
            patchState(store, {
                _id: userData.id,
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
         * Felhasználónevet adja vissza
         */
        getUsername(): string {
            return store._username();
        },
    })),
);
