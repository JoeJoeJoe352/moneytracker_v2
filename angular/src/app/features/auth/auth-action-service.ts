import { inject, Injectable, WritableSignal } from '@angular/core';
import { _, TranslateService } from '@ngx-translate/core';
import { AuthService } from './auth-service';
import { UserDataStore } from '../../shared/services/user-data-store';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LoginRequestData, RegisterRequestData } from './interfaces';

@Injectable({
    providedIn: 'root',
})
export class AuthActionService {
    private readonly authService = inject(AuthService);
    private readonly userDataStore = inject(UserDataStore);
    private readonly snackBar = inject(MatSnackBar);
    private readonly translateService = inject(TranslateService);

    /**
     * Felhasználó bejelentkezés kérésének kezelése
     */
    public login(
        params: LoginRequestData,
        loadingSignal: WritableSignal<boolean>,
        onSuccess: () => void,
    ): void {
        loadingSignal.set(true);

        this.authService.login(params).subscribe({
            next: () => {
                this.userDataStore.loadUserData({
                    username: params.username,
                    wallets: [], // TODO
                });
                this.snackBar.open(
                    this.translateService.instant(_('login.success')),
                    this.translateService.instant(_('etc.close')),
                );
                loadingSignal.set(false);
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

    /**
     * Felhasználó regisztráció kérésének lekezelése
     */
    public register(
        params: RegisterRequestData,
        loadingSignal: WritableSignal<boolean>,
        onSuccess: () => void,
    ): void {
        loadingSignal.set(true);

        this.authService.register(params).subscribe({
            next: () => {
                this.snackBar.open(
                    this.translateService.instant(_('register.success')),
                    this.translateService.instant(_('etc.close')),
                );
                onSuccess();
                loadingSignal.set(false);
            },
            error: (response) => {
                console.error('Ismeretlen hiba történt a regisztráció során!', response);
                this.snackBar.open(
                    this.translateService.instant(_('etc.general-error')),
                    this.translateService.instant(_('etc.close')),
                );
                loadingSignal.set(false);
            },
        });
    }
}
