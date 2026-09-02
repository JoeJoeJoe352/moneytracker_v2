import { Component, HostListener, inject } from '@angular/core';
import { AuthService } from '../../features/auth/auth-service';
import { Router, RouterLink } from '@angular/router';
import { UserDataStore } from '../services/user-data-store';
import { TranslatePipe } from '@ngx-translate/core';
import { LinkInterface } from '../interfaces';
import { HeaderLinkListComponent } from './header-link-list-component';
import { FormsModule } from '@angular/forms';
import { LanguageSwitcherComponent } from './language-switch-component';

@Component({
    selector: 'app-header',
    templateUrl: 'header.html',
    styleUrl: './header.scss',
    imports: [RouterLink, TranslatePipe, HeaderLinkListComponent, FormsModule, LanguageSwitcherComponent],
})
export class Header {
    private authService = inject(AuthService);
    private router = inject(Router);
    protected userData = inject(UserDataStore);

    /**
     * Hamburger menu lenyitása
     */
    protected menuOpen = false;

    /**
     * Publikusan elérhető linkek listája a fejlécben
     */
    protected publicLinkList: LinkInterface[] = [
        { url: '/', langKey: 'header.dashboard' },
        { url: '/welcome', langKey: 'header.welcome' },
    ];

    /**
     * Belépés után elérhető linkek listája a fejlécben
     */
    protected authenticatedLinkList: LinkInterface[] = [
        { url: '/transactions', langKey: 'header.transactions' },
        { url: '/wallets', langKey: 'header.wallets' },
    ];

    /**
     * Esc gomb listeren, mi történjen, ha esc-t nyom a user
     */
    @HostListener('document:keydown.escape')
    onEsc() {
        this.menuOpen = false;
    }

    /**
     * hamburger menu lenyitása/bezárása
     */
    toggleMenu(): void {
        this.menuOpen = !this.menuOpen;
    }

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
