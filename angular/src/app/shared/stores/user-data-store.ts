import { computed } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { UserData } from '../../features/auth/interfaces';
import { WalletDataInterfaceWithoutSum } from '../../features/wallet/interfaces';
import { db, User } from '../db/db';

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

/**
 * Walletek mentése az indexedDb-be (sum nélkül)
 * Tranzakción belül kell hívni, hogy a törlés és a mentés egyben történjen
 */
async function saveWalletsToDb(wallets: WalletDataInterfaceWithoutSum[]): Promise<void> {
    await db.wallet.clear();
    await db.wallet.bulkAdd(
        wallets.map(({ id, name, currencyCode, type }) => ({ id, name, currencyCode, type })),
    );
}

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
         * Beállítja a wallet adatokat (indexedDb-be is ment)
         */
        setWallets(wallets: WalletDataInterfaceWithoutSum[]): void {
            patchState(store, { _wallets: wallets });

            db.transaction('rw', db.wallet, () => saveWalletsToDb(wallets)).catch((error) =>
                console.error('failed to save wallets to local db', error),
            );
        },
    })),
    withMethods((store) => ({
        /**
         * Adatok alaphelyzetbe állítása (kijelentkezéskor)
         */
        resetData(): void {
            patchState(store, initialState);
            // indexedDb adatbázisok törlése
            db.transaction('rw', db.user, db.wallet, async () => {
                await db.user.clear();
                await db.wallet.clear();
            }).catch((error) => console.error('failed to clear local db', error));
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

            // a bejelentkezett usert és walletjait a lokális (IndexedDB) adatbázisba is elmentjük
            db.transaction('rw', db.user, db.wallet, async () => {
                await db.user.clear();
                await db.user.add({ id: userData.id, username: userData.username });
                await saveWalletsToDb(userData.wallets);
            }).catch((error) => console.error('failed to save user data to local db', error));
        },

        /**
         * A lokális (IndexedDB) adatbázisban tárolt user lekérése
         */
        getStoredUser(): Promise<User | undefined> {
            return db.user.toCollection().first();
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
