import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, input, output } from '@angular/core';
import { By } from '@angular/platform-browser';
import { Subject, throwError } from 'rxjs';
import { provideTranslateService } from '@ngx-translate/core';
import { MatDialogRef } from '@angular/material/dialog';
import { NotificationService } from '@shared/services/notification-service';
import { TransferTransactionModalComponent } from './transfer-transaction-modal';
import { TransferTransactionFormComponent } from './transfer-transaction-form-component';
import { TransferTransactionService } from './transfer-transaction-service';
import { TransferTransactionFormValue } from './interfaces';

@Component({
    selector: 'app-transfer-transaction-form-component',
    template: '',
})
class StubTransferTransactionFormComponent {
    isFormDisabled = input(false);
    saved = output<TransferTransactionFormValue>();
}

const formValue: TransferTransactionFormValue = {
    srcTransactionName: 'Megtakarításba',
    srcWalletId: 1,
    dstTransactionName: 'Napi költésből',
    dstWalletId: 2,
    transactionDate: new Date(2026, 10, 20),
    priceSum: 5000,
};

describe('TransferTransactionModalComponent (Vitest)', () => {
    let fixture: ComponentFixture<TransferTransactionModalComponent>;
    let serviceMock: { createTransferTransaction: ReturnType<typeof vi.fn> };
    let dialogRefMock: { close: ReturnType<typeof vi.fn> };
    let notificationMock: { show: ReturnType<typeof vi.fn> };

    beforeEach(async () => {
        serviceMock = { createTransferTransaction: vi.fn() };
        dialogRefMock = { close: vi.fn() };
        notificationMock = { show: vi.fn() };

        TestBed.configureTestingModule({
            imports: [TransferTransactionModalComponent],
            providers: [
                provideTranslateService(),
                { provide: TransferTransactionService, useValue: serviceMock },
                { provide: MatDialogRef, useValue: dialogRefMock },
                { provide: NotificationService, useValue: notificationMock },
            ],
        });
        TestBed.overrideComponent(TransferTransactionModalComponent, {
            remove: { imports: [TransferTransactionFormComponent] },
            add: { imports: [StubTransferTransactionFormComponent] },
        });
        await TestBed.compileComponents();

        fixture = TestBed.createComponent(TransferTransactionModalComponent);
        fixture.detectChanges();
    });

    function form(): StubTransferTransactionFormComponent {
        return fixture.debugElement.query(By.directive(StubTransferTransactionFormComponent))
            .componentInstance as StubTransferTransactionFormComponent;
    }

    it('should disable the form while saving, then close with true and notify on success', () => {
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

    it('should keep the modal open and re-enable the form when the save fails', () => {
        serviceMock.createTransferTransaction.mockReturnValue(throwError(() => new Error('400')));

        form().saved.emit(formValue);
        fixture.detectChanges();

        expect(form().isFormDisabled()).toBe(false);
        expect(dialogRefMock.close).not.toHaveBeenCalled();
        expect(notificationMock.show).not.toHaveBeenCalled();
    });
});
