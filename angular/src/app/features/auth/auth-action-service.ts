import { inject, Injectable, WritableSignal } from '@angular/core';
import { _ } from '@ngx-translate/core';
import { AuthService } from './auth-service';
import { UserDataStore } from '../../shared/services/user-data-store';
import { NotificationService } from '../../shared/services/notification-service';
import { LoginRequestData, RegisterRequestData } from './interfaces';
import { catchError, EMPTY, switchMap } from 'rxjs';

@Injectable({
    providedIn: 'root',
})
export class AuthActionService {
    private readonly authService = inject(AuthService);
    private readonly userDataStore = inject(UserDataStore);
    private readonly notification = inject(NotificationService);

    /**
     * Felhasználó bejelentkezés kérésének kezelése
     */
    public login(
        params: LoginRequestData,
        loadingSignal: WritableSignal<boolean>,
        onSuccess: () => void,
    ): void {
        loadingSignal.set(true);

        this.authService
            .login(params)
            .pipe(
                catchError((response) => {
                    if (response.status === 401) {
                        this.notification.showText(response.error.message);
                    } else {
                        console.error('Ismeretlen hiba történt a bejelentkezés során!', response);
                        this.notification.showGeneralError();
                    }
                    loadingSignal.set(false);
                    return EMPTY;
                }),
                switchMap(() => {
                    // User adatok lekérése, mert a login végpont csak a cookie-t állítja be
                    return this.authService.authenticateUser();
                }),
            )
            .subscribe({
                next: (userData) => {
                    this.userDataStore.loadUserData(userData);
                    this.notification.show(_('login.success'));
                    loadingSignal.set(false);
                    onSuccess();
                },
                error: (error) => {
                    this.userDataStore.resetData();
                    if (error.status !== 401) {
                        console.error('unknown error during authcheck!', error);
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
                this.notification.show(_('register.success'));
                onSuccess();
                loadingSignal.set(false);
            },
            error: (response) => {
                console.error('Ismeretlen hiba történt a regisztráció során!', response);
                this.notification.showGeneralError();
                loadingSignal.set(false);
            },
        });
    }
}
