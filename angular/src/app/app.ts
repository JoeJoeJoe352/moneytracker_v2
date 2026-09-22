import { Component, HostListener, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { Header } from './shared/components/layout/header';
import { ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { Sidebar } from './shared/components/layout/sidebar';

@Component({
    selector: 'app-root',
    templateUrl: './app.html',
    styleUrls: ['./app.scss', './shared/components/layout/mobile-menu.scss'],
    imports: [RouterOutlet, Header, ReactiveFormsModule, Sidebar, TranslatePipe],
})
export class App {
    private router = inject(Router);

    protected isMobileMenuOpen = signal(false);

    constructor() {
        this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe(() => {
            this.isMobileMenuOpen.set(false);
        });
    }

    protected toggleMobileMenu() {
        this.isMobileMenuOpen.set(!this.isMobileMenuOpen());
    }

    protected closeMobileMenu() {
        this.isMobileMenuOpen.set(false);
    }

    /**
     * Esc gomb listener, mi történjen, ha esc-t nyom a user
     */
    @HostListener('document:keydown.escape')
    protected onEsc() {
        this.isMobileMenuOpen.set(false);
    }
}
