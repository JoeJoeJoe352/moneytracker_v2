import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of, Subject, throwError } from 'rxjs';
import { AuthActionService } from './auth-action-service';
import { AuthService } from './auth-service';
import { UserDataStore } from '../../shared/services/user-data-store';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';
import { LoginRequestData, RegisterRequestData } from './interfaces';

describe('AuthActionService (Vitest)', () => {
    let service: AuthActionService;
    let authServiceMock: {
        login: ReturnType<typeof vi.fn>;
        register: ReturnType<typeof vi.fn>;
    };
    let userDataStoreMock: { loadUserData: ReturnType<typeof vi.fn> };
    let snackBarMock: { open: ReturnType<typeof vi.fn> };
    let consoleErrorSpy: ReturnType<typeof vi.spyOn>;

    const loginParams: LoginRequestData = { username: 'joe', password: 'secret' };
    const registerParams: RegisterRequestData = {
        username: 'joe',
        email: 'joe@example.com',
        password: 'secret',
    };

    beforeEach(() => {
        authServiceMock = {
            login: vi.fn(() => of({ message: 'ok' })),
            register: vi.fn(() => of({ message: 'ok' })),
        };
        userDataStoreMock = { loadUserData: vi.fn() };
        snackBarMock = { open: vi.fn() };
        consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);

        TestBed.configureTestingModule({
            providers: [
                AuthActionService,
                { provide: AuthService, useValue: authServiceMock },
                { provide: UserDataStore, useValue: userDataStoreMock },
                { provide: MatSnackBar, useValue: snackBarMock },
                // csak a kulcsot adjuk vissza, a fordítás tartalma nem tárgya ennek a tesztnek
                { provide: TranslateService, useValue: { instant: (key: unknown) => key } },
            ],
        });

        service = TestBed.inject(AuthActionService);
    });

    afterEach(() => {
        consoleErrorSpy.mockRestore();
    });

    describe('login', () => {
        it('should load the user data, show a success message and call onSuccess when the login succeeds', () => {
            const loading = signal(false);
            const onSuccess = vi.fn();

            service.login(loginParams, loading, onSuccess);

            expect(authServiceMock.login).toHaveBeenCalledWith(loginParams);
            expect(userDataStoreMock.loadUserData).toHaveBeenCalledWith({
                username: 'joe',
                wallets: [],
            });
            expect(snackBarMock.open).toHaveBeenCalledWith('login.success', 'etc.close');
            expect(onSuccess).toHaveBeenCalledTimes(1);
            expect(loading()).toBe(false);
        });

        it('should set the loading signal to true while the request is in flight', () => {
            authServiceMock.login.mockReturnValue(new Subject());
            const loading = signal(false);

            service.login(loginParams, loading, vi.fn());

            expect(loading()).toBe(true);
        });

        it('should show the backend error message and not log to console on a 401 error, without calling onSuccess', () => {
            authServiceMock.login.mockReturnValue(
                throwError(() => ({ status: 401, error: { message: 'Invalid credentials' } })),
            );
            const loading = signal(false);
            const onSuccess = vi.fn();

            service.login(loginParams, loading, onSuccess);

            expect(snackBarMock.open).toHaveBeenCalledWith('Invalid credentials', 'etc.close');
            expect(consoleErrorSpy).not.toHaveBeenCalled();
            expect(onSuccess).not.toHaveBeenCalled();
            expect(loading()).toBe(false);
        });

        it('should log the error and show a generic error message on a non-401 error', () => {
            const response = { status: 500 };
            authServiceMock.login.mockReturnValue(throwError(() => response));
            const loading = signal(false);
            const onSuccess = vi.fn();

            service.login(loginParams, loading, onSuccess);

            expect(consoleErrorSpy).toHaveBeenCalledWith(
                'Ismeretlen hiba történt a bejelentkezés során!',
                response,
            );
            expect(snackBarMock.open).toHaveBeenCalledWith('etc.general-error', 'etc.close');
            expect(onSuccess).not.toHaveBeenCalled();
            expect(loading()).toBe(false);
        });
    });

    describe('register', () => {
        it('should show a success message and call onSuccess when the registration succeeds', () => {
            const loading = signal(false);
            const onSuccess = vi.fn();

            service.register(registerParams, loading, onSuccess);

            expect(authServiceMock.register).toHaveBeenCalledWith(registerParams);
            expect(snackBarMock.open).toHaveBeenCalledWith('register.success', 'etc.close');
            expect(onSuccess).toHaveBeenCalledTimes(1);
            expect(loading()).toBe(false);
        });

        it('should set the loading signal to true while the request is in flight', () => {
            authServiceMock.register.mockReturnValue(new Subject());
            const loading = signal(false);

            service.register(registerParams, loading, vi.fn());

            expect(loading()).toBe(true);
        });

        it('should log the error, show a generic error message and not call onSuccess on failure', () => {
            const response = { status: 500 };
            authServiceMock.register.mockReturnValue(throwError(() => response));
            const loading = signal(false);
            const onSuccess = vi.fn();

            service.register(registerParams, loading, onSuccess);

            expect(consoleErrorSpy).toHaveBeenCalledWith(
                'Ismeretlen hiba történt a regisztráció során!',
                response,
            );
            expect(snackBarMock.open).toHaveBeenCalledWith('etc.general-error', 'etc.close');
            expect(onSuccess).not.toHaveBeenCalled();
            expect(loading()).toBe(false);
        });
    });
});
