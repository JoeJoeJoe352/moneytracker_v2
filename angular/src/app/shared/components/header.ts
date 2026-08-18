import { Component, HostListener, inject } from '@angular/core';
import { AuthService } from '../../features/auth/auth-service';
import { Router, RouterLink } from '@angular/router';
import { UserDataStore } from '../services/user-data-store';
import { TranslatePipe } from '@ngx-translate/core';

interface LinkInterface {
    url: string;
    langKey: string;
}

@Component({
    selector: 'app-header',
    templateUrl: 'header.html',
    styleUrl: './header.scss',
    imports: [RouterLink, TranslatePipe],
})
export class Header {
    private authService = inject(AuthService);
    private router = inject(Router);
    protected userData = inject(UserDataStore);

    /**
     * Hamburger menu lenyitása
     */
    protected menuOpen = false;

    protected publicLinkList: LinkInterface[] = [
        { url: '/', langKey: 'header.dashboard' },
        { url: '/welcome', langKey: 'header.welcome' },
    ];

    protected authenticatedLinkList: LinkInterface[] = [
        {url: "/transactions", langKey: "header.transactions"}
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
