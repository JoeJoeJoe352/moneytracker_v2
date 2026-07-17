import { Component, inject, signal } from '@angular/core';
import { RegisterModalComponent } from '../auth/register-modal';
import { UserDataStore } from '../../shared/services/user-data-store';
import { TranslatePipe } from '@ngx-translate/core';
import { LoginModalComponent } from '../auth/login.modal';

@Component({
    selector: 'app-welcome',
    templateUrl: './welcome.html',
    imports: [LoginModalComponent, RegisterModalComponent, TranslatePipe],
    standalone: true,
})
export class Welcome {
    protected userData = inject(UserDataStore);

    protected isLoginModalOpen = signal(false);
    protected isRegisterModalOpen = signal(false);

    openLoginModal() {
        this.isLoginModalOpen.set(true);
    }

    openRegisterModal() {
        this.isRegisterModalOpen.set(true);
    }

    closeRegisterModal() {
        this.isRegisterModalOpen.set(false);
    }

    closeLoginModal() {
        this.isLoginModalOpen.set(false);
    }
}
