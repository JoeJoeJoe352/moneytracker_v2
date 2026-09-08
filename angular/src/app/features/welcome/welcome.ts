import { Component, inject, signal } from '@angular/core';
import { UserDataStore } from '../../shared/services/user-data-store';
import { TranslatePipe } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { LoginComponent } from '../auth/login-component';
import { AuthActionService } from '../auth/auth-action-service';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { Router } from '@angular/router';
import { RegisterComponent } from '../auth/register-component';

@Component({
    selector: 'app-welcome',
    templateUrl: './welcome.html',
    imports: [TranslatePipe, MatCardModule, MatButtonModule],
    standalone: true,
})
export class Welcome {
    private readonly dialog = inject(MatDialog);
    private readonly actionService = inject(AuthActionService);
    private readonly router = inject(Router);
    protected readonly userData = inject(UserDataStore);

    /**
     * Töltődés alatt van-e valamelyik form
     */
    protected isloading = signal(false);

    /**
     * Login modal felnyitása
     */
    openLoginModal() {
        const dialogRef = this.dialog.open(LoginComponent, {
            width: '500px',
            data: {
                isloading: this.isloading,
            },
        });

        dialogRef.componentInstance.login.subscribe((payload) =>
            this.actionService.login(payload, this.isloading, () => {
                dialogRef.close();
                this.router.navigate(['/']);
            }),
        );
    }

    /**
     * Regisztrációs modal felnyitása
     */
    openRegisterModal() {
        const dialogRef = this.dialog.open(RegisterComponent, {
            width: '500px',
            data: {
                isloading: this.isloading,
            },
        });

        dialogRef.componentInstance.register.subscribe((payload) =>
            this.actionService.register(payload, this.isloading, () => {
                dialogRef.close();
                this.router.navigate(['/']);
            }),
        );
    }
}
