import { Component, EventEmitter, Output, Signal } from '@angular/core';
import { BaseModal } from '../../shared/components/modal/base-modal';
import { LoginComponent } from './login-component';
import { translate } from '@ngx-translate/core';

@Component({
    selector: 'app-login-modal',
    template: `
        <app-base-modal [title]="modalTitle()" (closeModal)="closeModal.emit()">
            <app-login-component (closeModal)="closeModal.emit()" />
        </app-base-modal>
    `,
    imports: [BaseModal, LoginComponent],
})
export class LoginModalComponent {
    @Output() closeModal = new EventEmitter<void>();

    protected modalTitle = translate('login.title') as Signal<string>;
}
