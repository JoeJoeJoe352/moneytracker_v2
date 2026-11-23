import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TransferTransactionService } from './transfer-transaction-service';

describe('TransferTransactionService (Vitest)', () => {
    let service: TransferTransactionService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting()],
        });
        service = TestBed.inject(TransferTransactionService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpMock.verify());

    it('should post the transfer with the date formatted as yyyy-MM-dd', () => {
        service
            .createTransferTransaction({
                srcTransactionName: 'Megtakarításba',
                srcWalletId: 1,
                dstTransactionName: 'Napi költésből',
                dstWalletId: 2,
                transactionDate: new Date(2026, 10, 5),
                priceSum: 5000,
            })
            .subscribe();

        const request = httpMock.expectOne('/api/transfer-transaction');
        expect(request.request.method).toBe('POST');
        expect(request.request.body).toEqual({
            srcTransactionName: 'Megtakarításba',
            srcWalletId: 1,
            dstTransactionName: 'Napi költésből',
            dstWalletId: 2,
            transactionDate: '2026-11-05',
            priceSum: 5000,
        });
        request.flush(null);
    });
});
