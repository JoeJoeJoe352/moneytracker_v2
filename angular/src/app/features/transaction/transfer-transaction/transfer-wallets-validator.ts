import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { WalletDataInterface } from '@app/features/wallet/interfaces';

/**
 * Form szintű validátor az átvezetés két walletjére: nem lehet ugyanaz a wallet
 * (sameWallet), és a pénznemüknek egyeznie kell (differentCurrency). A backend is
 * ellenőrzi mindkettőt
 *
 * @param getWallets a user walletjeit adja vissza (a pénznem összevetéséhez)
 */
export function transferWalletsValidator(getWallets: () => WalletDataInterface[]): ValidatorFn {
    return (group: AbstractControl): ValidationErrors | null => {
        const srcWalletId: unknown = group.get('srcWalletId')?.value;
        const dstWalletId: unknown = group.get('dstWalletId')?.value;

        if (srcWalletId === null || dstWalletId === null) {
            return null;
        }
        if (srcWalletId === dstWalletId) {
            return { sameWallet: true };
        }

        const wallets = getWallets();
        const srcWallet = wallets.find((wallet) => wallet.id === srcWalletId);
        const dstWallet = wallets.find((wallet) => wallet.id === dstWalletId);

        if (srcWallet && dstWallet && srcWallet.currencyCode !== dstWallet.currencyCode) {
            return { differentCurrency: true };
        }
        return null;
    };
}
