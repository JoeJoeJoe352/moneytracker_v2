import { Component, inject } from '@angular/core';
import { MatToolbar } from '@angular/material/toolbar';
import { LanguageSwitcherComponent } from './language-switch-component';
import { MobileMenuButtonComponent } from './mobile-menu-button-component';
import { RouterLink } from '@angular/router';
import { LayoutStore } from './layout-store';

/**
 * Fejléc menü megjelenítésére szolgáló komponens
 */
@Component({
    selector: 'app-header',
    templateUrl: './header-component.html',
    styleUrl: './header-component.scss',
    imports: [MatToolbar, LanguageSwitcherComponent, MobileMenuButtonComponent, RouterLink],
})
export class HeaderComponent {
    protected readonly layoutStore = inject(LayoutStore);
}
