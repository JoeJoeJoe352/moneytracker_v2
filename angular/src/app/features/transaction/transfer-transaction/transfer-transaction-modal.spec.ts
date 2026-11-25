import { describe, it, expect, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, input, output } from '@angular/core';
import { By } from '@angular/platform-browser';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, of, Subject, throwError } from 'rxjs';
import { provideTranslateService } from '@ngx-translate/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { NotificationService } from '@shared/services/notification-service';
import { TransferTransactionModalComponent } from './transfer-transaction-modal';
import { TransferTransactionFormComponent } from './transfer-transaction-form-component';
import { TransferTransactionService } from './transfer-transaction-service';
import { TransferTransactionFormValue, TransferTransactionResponse } from './interfaces';

@Component({
    selector: 'app-transfer-transaction-form-component',
    template: '',
})
class StubTransferTransactionFormComponent {
    isFormDisabled = input(false);
    transfer = input<TransferTransactionResponse | null>(null);
    saved = output<TransferTransactionFormValue>();
    deleted = output<void>();
}

const formValue: TransferTransactionFormValue = {
    srcTransactionName: 'Megtakarításba',
    srcWalletId: 1,
    dstTransactionName: 'Napi költésből',
    dstWalletId: 2,
    transactionDate: new Date(2026, 10, 20),
    priceSum: 5000,
};

const existingTransfer: TransferTransactionResponse = {
    transferId: 7,
    srcTransactionName: 'Megtakarításba',
    srcWalletId: 1,
    dstTransactionName: 'Napi költésből',
    dstWalletId: 2,
    transactionDate: '2026-11-01',
    priceSum: 1000,
};

describe('TransferTransactionModalComponent (Vitest)', () => {
    let fixture: ComponentFixture<TransferTransactionModalComponent>;
    let serviceMock: {
        createTransferTransaction: ReturnType<typeof vi.fn>;
        getTransferTransactionByTransactionId: ReturnType<typeof vi.fn>;
        updateTransferTransaction: ReturnType<typeof vi.fn>;
        deleteTransferTransaction: ReturnType<typeof vi.fn>;
    };
    let dialogRefMock: { close: ReturnType<typeof vi.fn> };
    let notificationMock: { show: ReturnType<typeof vi.fn> };
    let confirmResult: boolean;

    /**
     * @param transactionId a modalnak átadott tranzakció id, null esetén új átvezetés
     * @param loadResponse  az átvezetés betöltésének válasza (szerkesztéskor)
     */
    async function setup(
        transactionId: number | null,
        loadResponse: Observable<TransferTransactionResponse> = of(existingTransfer),
    ): Promise<void> {
        serviceMock = {
            createTransferTransaction: vi.fn(),
            getTransferTransactionByTransactionId: vi.fn(() => loadResponse),
            updateTransferTransaction: vi.fn(),
            deleteTransferTransaction: vi.fn(),
        };
        dialogRefMock = { close: vi.fn() };
        notificationMock = { show: vi.fn() };
        confirmResult = true;

        TestBed.configureTestingModule({
            imports: [TransferTransactionModalComponent],
            providers: [
                provideTranslateService(),
                { provide: TransferTransactionService, useValue: serviceMock },
                { provide: MatDialogRef, useValue: dialogRefMock },
                { provide: MAT_DIALOG_DATA, useValue: { transactionId } },
                { provide: NotificationService, useValue: notificationMock },
            ],
        });
        TestBed.overrideComponent(TransferTransactionModalComponent, {
            remove: { imports: [TransferTransactionFormComponent] },
            add: { imports: [StubTransferTransactionFormComponent] },
        });
        // A komponens importálja a MatDialogModule-t, ami saját MatDialog-ot ad, ezért providers helyett
        // overrideProvider kell. A törlés megerősítő dialog eredményét a confirmResult adja
        TestBed.overrideProvider(MatDialog, {
            useValue: { open: vi.fn(() => ({ afterClosed: () => of(confirmResult) })) },
        });
        await TestBed.compileComponents();

        fixture = TestBed.createComponent(TransferTransactionModalComponent);
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();
    }

    function form(): StubTransferTransactionFormComponent {
        return fixture.debugElement.query(By.directive(StubTransferTransactionFormComponent))
            .componentInstance as StubTransferTransactionFormComponent;
    }

    it('should not load anything and pass no transfer to the form when creating a new transfer', async () => {
        await setup(null);

        expect(serviceMock.getTransferTransactionByTransactionId).not.toHaveBeenCalled();
        expect(form().transfer()).toBeNull();
        expect(fixture.nativeElement.querySelector('h2').textContent).toContain(
            'transfer_transaction.create',
        );
    });

    it('should disable the form while saving, then close with true and notify on create success', async () => {
        await setup(null);
        const response = new Subject<void>();
        serviceMock.createTransferTransaction.mockReturnValue(response);

        form().saved.emit(formValue);
        fixture.detectChanges();

        expect(serviceMock.createTransferTransaction).toHaveBeenCalledWith(formValue);
        expect(form().isFormDisabled()).toBe(true);
        expect(dialogRefMock.close).not.toHaveBeenCalled();

        response.next();
        response.complete();
        fixture.detectChanges();

        expect(form().isFormDisabled()).toBe(false);
        expect(notificationMock.show).toHaveBeenCalledWith('etc.save-success');
        expect(dialogRefMock.close).toHaveBeenCalledWith(true);
    });

    it('should keep the modal open and re-enable the form when the save fails', async () => {
        await setup(null);
        serviceMock.createTransferTransaction.mockReturnValue(throwError(() => new Error('400')));

        form().saved.emit(formValue);
        fixture.detectChanges();

        expect(form().isFormDisabled()).toBe(false);
        expect(dialogRefMock.close).not.toHaveBeenCalled();
        expect(notificationMock.show).not.toHaveBeenCalled();
    });

    it('should load the transfer by the transaction id and pass it to the form in edit mode', async () => {
        await setup(10);

        expect(serviceMock.getTransferTransactionByTransactionId).toHaveBeenCalledWith(10);
        expect(form().transfer()).toEqual(existingTransfer);
        expect(fixture.nativeElement.querySelector('h2').textContent).toContain(
            'transfer_transaction.update',
        );
    });

    it('should update the loaded transfer by its transfer id on save', async () => {
        await setup(10);
        serviceMock.updateTransferTransaction.mockReturnValue(of(undefined));

        form().saved.emit(formValue);
        fixture.detectChanges();

        expect(serviceMock.updateTransferTransaction).toHaveBeenCalledWith(7, formValue);
        expect(serviceMock.createTransferTransaction).not.toHaveBeenCalled();
        expect(dialogRefMock.close).toHaveBeenCalledWith(true);
    });

    it('should delete the transfer by its transfer id after confirmation', async () => {
        await setup(10);
        serviceMock.deleteTransferTransaction.mockReturnValue(of(undefined));

        form().deleted.emit();
        fixture.detectChanges();

        expect(serviceMock.deleteTransferTransaction).toHaveBeenCalledWith(7);
        expect(notificationMock.show).toHaveBeenCalledWith('etc.delete-success');
        expect(dialogRefMock.close).toHaveBeenCalledWith(true);
    });

    it('should not delete when the confirmation is cancelled', async () => {
        await setup(10);
        confirmResult = false;

        form().deleted.emit();
        fixture.detectChanges();

        expect(serviceMock.deleteTransferTransaction).not.toHaveBeenCalled();
        expect(dialogRefMock.close).not.toHaveBeenCalled();
    });

    it('should close the modal and notify when the transfer is not found', async () => {
        await setup(
            10,
            throwError(() => new HttpErrorResponse({ status: 404 })),
        );

        expect(dialogRefMock.close).toHaveBeenCalled();
        expect(notificationMock.show).toHaveBeenCalledWith('transaction.not-found');
    });
});
