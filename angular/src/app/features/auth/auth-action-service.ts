import { inject, Injectable, WritableSignal } from '@angular/core';
import { _, TranslateService } from '@ngx-translate/core';
import { AuthService } from './auth-service';
import { UserDataStore } from '../../shared/services/user-data-store';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LoginRequestData } from './interfaces';

@Injectable({
    providedIn: 'root',
})
export class AuthActionService {
    private readonly authService = inject(AuthService);
    private readonly userDataStore = inject(UserDataStore);
    private readonly router = inject(Router);
    private readonly snackBar = inject(MatSnackBar);
    private readonly translateService = inject(TranslateService);

    public login(
        params: LoginRequestData,
        loadingSignal: WritableSignal<boolean>,
        onSuccess: () => void,
    ) {
        loadingSignal.set(true);

        this.authService.login(params).subscribe({
            next: () => {
                this.userDataStore.loadUserData({
                    username: params.username,
                    wallets: [], // TODO
                });
                onSuccess();
            },
            error: (response) => {
                if (response.status === 401) {
                    this.snackBar.open(
                        response.error.message,
                        this.translateService.instant(_('etc.close')),
                    );
                } else {
                    console.error('Ismeretlen hiba történt a bejelentkezés során!', response);
                    this.snackBar.open(
                        this.translateService.instant(_('etc.general-error')),
                        this.translateService.instant(_('etc.close')),
                    );
                }
                loadingSignal.set(false);
            },
        });
    }
}
