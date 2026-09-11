import { Component, inject } from '@angular/core';
import { AuthService } from '../../features/auth/auth-service';
import { Router, RouterLink } from '@angular/router';
import { UserDataStore } from '../services/user-data-store';
import { LinkInterface } from '../interfaces';
import { TranslatePipe } from '@ngx-translate/core';
import { HeaderLinkListComponent } from './header-link-list-component';
import { LanguageSwitcherComponent } from './language-switch-component';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';

@Component({
    selector: 'app-sidebar',
    templateUrl: 'sidebar.html',
    styleUrl: './sidebar.scss',
    imports: [
        RouterLink,
        TranslatePipe,
        HeaderLinkListComponent,
        LanguageSwitcherComponent,
        MatIconButton,
        MatIcon,
    ],
})
export class Sidebar {
    private authService = inject(AuthService);
    private router = inject(Router);
    protected userData = inject(UserDataStore);

    /**
     * Publikusan elérhető linkek listája a fejlécben
     */
    protected publicLinkList: LinkInterface[] = [
        { url: '/', langKey: 'header.dashboard', icon: 'home' },
        { url: '/welcome', langKey: 'header.welcome', icon: 'waving_hand' },
    ];

    /**
     * Belépés után elérhető linkek listája a fejlécben
     */
    protected authenticatedLinkList: LinkInterface[] = [
        { url: '/transactions', langKey: 'header.transactions', icon: 'sync_alt' },
        { url: '/wallets', langKey: 'header.wallets', icon: 'account_balance_wallet' },
    ];

    /**
     * Felhasználó kijelentkeztetése
     */
    logout(): void {
        this.authService.logout().subscribe({
            next: () => {
                this.userData.resetData();
                this.router.navigate(['/welcome']); // ez az utolsó mindig
            },
            error: (response) => {
                console.error(response);
                //TODO: új toast: this.snackbar.open('Error during logout', 'close');
            },
        });
    }
}
