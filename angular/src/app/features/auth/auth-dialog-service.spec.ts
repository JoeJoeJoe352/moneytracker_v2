import { describe, it, expect, vi, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { Subject } from 'rxjs';
import { AuthDialogService } from './auth-dialog-service';
import { AuthActionService } from './auth-action-service';
import { LoginComponent } from './login-component';
import { RegisterComponent } from './register-component';
import { LoginRequestData, RegisterRequestData } from './interfaces';

describe('AuthDialogService (Vitest)', () => {
    let service: AuthDialogService;
    let loginEmitter: Subject<LoginRequestData>;
    let registerEmitter: Subject<RegisterRequestData>;
    let dialogRefMock: {
        componentInstance: {
            login: Subject<LoginRequestData>;
            register: Subject<RegisterRequestData>;
        };
        close: ReturnType<typeof vi.fn>;
    };
    let dialogMock: { open: ReturnType<typeof vi.fn> };
    let actionServiceMock: { login: ReturnType<typeof vi.fn>; register: ReturnType<typeof vi.fn> };
    let routerMock: { navigate: ReturnType<typeof vi.fn> };

    const loginParams: LoginRequestData = { email: 'joe@asd.com', password: 'secret' };
    const registerParams: RegisterRequestData = {
        username: 'joe',
        email: 'joe@asd.com',
        password: 'secret',
    };

    beforeEach(() => {
        loginEmitter = new Subject();
        registerEmitter = new Subject();
        dialogRefMock = {
            componentInstance: { login: loginEmitter, register: registerEmitter },
            close: vi.fn(),
        };
        dialogMock = { open: vi.fn(() => dialogRefMock) };
        // az onSuccess callbacket azonnal meghívjuk, mintha sikeres lett volna a kérés
        actionServiceMock = {
            login: vi.fn((_params, _loading, onSuccess: () => void) => onSuccess()),
            register: vi.fn((_params, _loading, onSuccess: () => void) => onSuccess()),
        };
        routerMock = { navigate: vi.fn(() => Promise.resolve(true)) };

        TestBed.configureTestingModule({
            providers: [
                AuthDialogService,
                { provide: MatDialog, useValue: dialogMock },
                { provide: AuthActionService, useValue: actionServiceMock },
                { provide: Router, useValue: routerMock },
            ],
        });

        service = TestBed.inject(AuthDialogService);
    });

    it('should open the login dialog with the shared loading signal', () => {
        service.openLogin();

        expect(dialogMock.open).toHaveBeenCalledWith(LoginComponent, {
            restoreFocus: true,
            width: '500px',
            data: { isLoading: service.isLoading },
        });
    });

    it('should log in, close the dialog and navigate to the dashboard when the login form is submitted', () => {
        service.openLogin();

        loginEmitter.next(loginParams);

        expect(actionServiceMock.login).toHaveBeenCalledWith(
            loginParams,
            service.isLoading,
            expect.any(Function),
        );
        expect(dialogRefMock.close).toHaveBeenCalledOnce();
        expect(routerMock.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should not close the login dialog when the login does not succeed', () => {
        actionServiceMock.login.mockImplementation(() => undefined);
        service.openLogin();

        loginEmitter.next(loginParams);

        expect(dialogRefMock.close).not.toHaveBeenCalled();
        expect(routerMock.navigate).not.toHaveBeenCalled();
    });

    it('should open the register dialog with the shared loading signal', () => {
        service.openRegister();

        expect(dialogMock.open).toHaveBeenCalledWith(RegisterComponent, {
            restoreFocus: true,
            width: '500px',
            data: { isLoading: service.isLoading },
        });
    });

    it('should register and close the dialog without navigating when the register form is submitted', () => {
        service.openRegister();

        registerEmitter.next(registerParams);

        expect(actionServiceMock.register).toHaveBeenCalledWith(
            registerParams,
            service.isLoading,
            expect.any(Function),
        );
        expect(dialogRefMock.close).toHaveBeenCalledOnce();
        expect(routerMock.navigate).not.toHaveBeenCalled();
    });
});
