import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
    WalletDataInterface,
    WalletCreateRequest,
    WalletUpdateRequest,
    WalletSyncData,
    WalletSyncDataToBackend,
} from './interfaces';

@Injectable({
    providedIn: 'root',
})
export class WalletService {
    private readonly http = inject(HttpClient);

    /**
     * Visszatér a felhasználó walletjaival
     */
    listWallets(): Observable<WalletDataInterface[]> {
        return this.http.get<WalletDataInterface[]>('/api/wallet');
    }

    /**
     * Elment egy walletet
     */
    createWallet(wallet: WalletCreateRequest): Observable<void> {
        return this.http.post<void>('/api/wallet', wallet);
    }

    /**
     * Update-eli a walletet
     */
    updateWallet(id: number, wallet: WalletUpdateRequest): Observable<void> {
        return this.http.put<void>('/api/wallet/' + id, wallet);
    }

    /**
     * Soft delete-eli a walletet
     */
    softDeleteWallet(id: number): Observable<void> {
        return this.http.delete<void>('/api/wallet/' + id);
    }

    /**
     * Balance szinkronizációs kérést indít a backend felé
     */
    syncWallet(data: WalletSyncData): Observable<void> {
        const walletDataWithDate: WalletSyncDataToBackend = {
            currentBalance: data.currentBalance,
            syncDate: new Date().toLocaleDateString('sv-SE'),
        };

        return this.http.post<void>('/api/wallet/' + data.walletId + '/sync', walletDataWithDate);
    }
}
