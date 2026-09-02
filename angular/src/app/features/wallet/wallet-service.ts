import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { WalletDataInterface, WalletCreateRequest, WalletUpdateRequest } from './interfaces';

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
     * Visszaadja a user egy walletjét id alapján
     */
    getWalletById(id: number): Observable<WalletDataInterface> {
        return this.http.get<WalletDataInterface>('/api/wallet/' + id);
    }

    /**
     * Soft delete-eli a walletet
     */
    softDeleteWallet(id: number): Observable<void> {
        return this.http.delete<void>('/api/wallet/' + id);
    }
}
