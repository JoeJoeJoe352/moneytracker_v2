import { Component, computed, inject, Signal } from '@angular/core';
import { AuthService } from '../../features/auth/auth-service';
import { Router, RouterLink } from '@angular/router';
import { LinkInterface } from './interfaces';
import { _, TranslatePipe } from '@ngx-translate/core';
import { MenuLinkComponent } from './menu-link-component';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { AuthDialogService } from '../../features/auth/auth-dialog-service';
import { NotificationService } from '../../shared/services/notification-service';
import { UserDataStore } from '@app/shared/stores/user-data-store';

@Component({
    selector: 'app-sidebar',
    templateUrl: './sidebar-component.html',
    styleUrl: './sidebar-component.scss',
    imports: [RouterLink, TranslatePipe, MenuLinkComponent, MatIconButton, MatIcon],
})
export class SidebarComponent {
    private readonly authDialog = inject(AuthDialogService);
    private readonly authService = inject(AuthService);
    private readonly router = inject(Router);
    private readonly notification = inject(NotificationService);

    protected userData = inject(UserDataStore);

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
            links.push({
                url: '/recurring-transactions',
                langKey: 'header.recurring_transactions',
                icon: 'event_repeat',
            });
        } else {
            links.push({
                langKey: 'header.login',
                icon: 'login',
                action: () => this.authDialog.openLogin(),
            });
            links.push({
                langKey: 'header.register',
                icon: 'how_to_reg',
                action: () => this.authDialog.openRegister(),
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
                void this.router.navigate(['/welcome']);
            },
            error: (response) => {
                console.error(response);
                this.notification.showGeneralError();
            },
        });
    }
}
