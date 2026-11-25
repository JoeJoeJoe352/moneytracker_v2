import { Component, computed, DOCUMENT, effect, HostListener, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { UserDataStore } from './shared/stores/user-data-store';
import { HeaderComponent } from './core/layout/header-component';
import { ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { SidebarComponent } from './core/layout/sidebar-component';

@Component({
    selector: 'app-root',
    templateUrl: './app.html',
    styleUrls: ['./app.scss', './core/layout/mobile-menu.scss'],
    imports: [RouterOutlet, HeaderComponent, ReactiveFormsModule, SidebarComponent, TranslatePipe],
})
export class App {
    private router = inject(Router);
    private readonly document = inject(DOCUMENT);

    private readonly userData = inject(UserDataStore);

    protected isMobileMenuOpen = signal(false);

    /**
     * Aktuális URL útvonala (query és fragment nélkül)
     */
    private currentPath = toSignal(
        this.router.events.pipe(
            filter((event) => event instanceof NavigationEnd),
            map((event) => event.urlAfterRedirects.split(/[?#]/)[0]),
        ),
        { initialValue: '' },
    );

    /**
     * Kijelentkezett user a welcome (landing) oldalon nem látja az oldalsávot
     */
    protected isSidebarHidden = computed(
        () => this.currentPath() === '/welcome' && !this.userData.isUserLogged(),
    );

    constructor() {
        // Nyitott mobil menü alatt letiltjuk az oldal görgetését
        effect(() => {
            this.document.documentElement.classList.toggle(
                'mobile-menu-scroll-lock',
                this.isMobileMenuOpen(),
            );
        });

        this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe(() => {
            this.isMobileMenuOpen.set(false);
        });
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
