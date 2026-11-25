import { inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { AuthActionService } from './auth-action-service';
import { LoginComponent } from './login-component';
import { RegisterComponent } from './register-component';

/**
 * Login és regisztrációs modalok megnyitása, hogy több helyről (sidebar, welcome oldal) is elérhetők legyenek
 */
@Injectable({ providedIn: 'root' })
export class AuthDialogService {
    private readonly actionService = inject(AuthActionService);
    private readonly router = inject(Router);
    private readonly dialog = inject(MatDialog);

    /**
     * Töltődés alatt van-e valamelyik form
     */
    readonly isLoading = signal(false);

    /**
     * Login modal felnyitása
     */
    openLogin(): void {
        const dialogRef = this.dialog.open(LoginComponent, {
            restoreFocus: true,
            width: '500px',
            data: {
                isLoading: this.isLoading,
            },
        });

        dialogRef.componentInstance.login.subscribe((payload) =>
            this.actionService.login(payload, this.isLoading, () => {
                dialogRef.close();
                void this.router.navigate(['/']);
            }),
        );
    }

    /**
     * Regisztrációs modal felnyitása
     */
    openRegister(): void {
        const dialogRef = this.dialog.open(RegisterComponent, {
            restoreFocus: true,
            width: '500px',
            data: {
                isLoading: this.isLoading,
            },
        });

        dialogRef.componentInstance.register.subscribe((payload) =>
            this.actionService.register(payload, this.isLoading, () => {
                dialogRef.close();
            }),
        );
    }
}
