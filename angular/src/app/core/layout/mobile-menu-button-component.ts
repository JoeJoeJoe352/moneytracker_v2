import { Component, inject } from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';
import { LayoutStore } from './layout-store';

/**
 * Mobilnézeten megjelenő hamburger gomb, ami az oldalsáv menüt nyitja/zárja
 */
@Component({
    selector: 'app-mobile-menu-button',
    templateUrl: './mobile-menu-button-component.html',
    styleUrl: './mobile-menu-button-component.scss',
    imports: [MatIconButton, MatIcon, TranslatePipe],
})
export class MobileMenuButtonComponent {
    protected readonly layoutStore = inject(LayoutStore);
}
