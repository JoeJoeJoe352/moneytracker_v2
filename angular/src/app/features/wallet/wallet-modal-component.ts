import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { BaseModal } from '../../shared/components/modal/base-modal';
import { WalletFormComponent } from './wallet-form-component';
import { WalletCreateRequest, WalletDataInterface, WalletUpdateRequest } from './interfaces';

@Component({
    selector: 'app-wallet-modal-component',
    template: `
        <app-base-modal
            [title]="wallet ? ('wallet.update' | translate) : ('wallet.create' | translate)"
            (closeModal)="closeModal.emit()"
        >
            @if (isDataInitializing) {
                <div class="text-center">
                    <div class="spinner-border" role="status"></div>
                </div>
            } @else {
                <app-wallet-form-component
                    [wallet]="wallet"
                    [isFormDisabled]="isFormDisabled"
                    (saved)="saved.emit($event)"
                    (deleted)="deleted.emit($event)"
                />
            }
        </app-base-modal>
    `,
    standalone: true,
    imports: [BaseModal, WalletFormComponent, TranslatePipe],
})
export class WalletModalComponent {
    @Input() wallet: WalletDataInterface | null = null;
    @Input({ required: true }) isFormDisabled!: boolean;
    @Input({ required: true }) isDataInitializing!: boolean;

    @Output() closeModal = new EventEmitter<void>();
    @Output() saved = new EventEmitter<WalletCreateRequest | WalletUpdateRequest>();
    @Output() deleted = new EventEmitter<number>();
}
