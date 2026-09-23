import { Component, computed, inject, Signal, signal } from '@angular/core';
import { AuthService } from '../../../features/auth/auth-service';
import { Router, RouterLink } from '@angular/router';
import { LinkInterface } from './interfaces';
import { _, TranslatePipe } from '@ngx-translate/core';
import { HeaderLinkListComponent } from './header-link-list-component';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { AuthActionService } from '../../../features/auth/auth-action-service';
import { MatDialog } from '@angular/material/dialog';
import { LoginComponent } from '../../../features/auth/login-component';
import { RegisterComponent } from '../../../features/auth/register-component';
import { NotificationService } from '../../services/notification-service';
import { UserDataStore } from '@app/shared/stores/user-data-store';

@Component({
    selector: 'app-sidebar',
    templateUrl: './sidebar-component.html',
    styleUrl: './sidebar-component.scss',
    imports: [RouterLink, TranslatePipe, HeaderLinkListComponent, MatIconButton, MatIcon],
})
export class SidebarComponent {
    private readonly actionService = inject(AuthActionService);
    private readonly authService = inject(AuthService);
    private readonly router = inject(Router);
    private readonly dialog = inject(MatDialog);
    private readonly notification = inject(NotificationService);

    protected userData = inject(UserDataStore);

    /**
     * Töltődés alatt van-e valamelyik form
     */
    protected isLoading = signal(false);

    /**
     * Összeállítja a linkek listáját, amit a user láthat az oldalsávban
     */
    protected linkList: Signal<LinkInterface[]> = computed(() => {
        const links: LinkInterface[] = [
            { url: '/', langKey: 'header.dashboard', icon: 'home' },
            { url: '/welcome', langKey: 'header.welcome', icon: 'waving_hand' },
        ];

        if (this.userData.isUserLogged()) {
            links.push({ url: '/transactions', langKey: 'header.transactions', icon: 'sync_alt' });
            links.push({
                url: '/wallets',
                langKey: 'header.wallets',
                icon: 'account_balance_wallet',
            });
        } else {
            links.push({ langKey: 'header.login', icon: 'login', action: this.openLoginModal });
            links.push({
                langKey: 'header.register',
                icon: 'how_to_reg',
                action: this.openRegisterModal,
            });
        }

        return links;
    });

    /**
     * Felhasználó kijelentkeztetése
     */
    logout(): void {
        this.authService.logout().subscribe({
            next: () => {
                this.userData.resetData();
                this.notification.show(_('header.logout.success'));
                this.router.navigate(['/welcome']);
            },
            error: (response) => {
                console.error(response);
                this.notification.showGeneralError();
            },
        });
    }

    /**
     * Login modal felnyitása
     */
    openLoginModal = () => {
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
                this.router.navigate(['/']);
            }),
        );
    };

    /**
     * Regisztrációs modal felnyitása
     */
    openRegisterModal = () => {
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
    };
}
