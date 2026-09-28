import { Component, model } from '@angular/core';
import { MatToolbar } from '@angular/material/toolbar';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { LanguageSwitcherComponent } from './language-switch-component';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'app-header',
    templateUrl: './header-component.html',
    styleUrls: ['./header-component.scss', './mobile-menu.scss'],
    imports: [MatToolbar, MatIconButton, MatIcon, LanguageSwitcherComponent, TranslatePipe],
})
export class HeaderComponent {
    /**
     * Nyitva van-e a mobile menü
     */
    isMobileMenuOpen = model(false)

    /**
     * hamburger menu lenyitása/bezárása
     */
    toggleMenu(): void {
        this.isMobileMenuOpen.update((open) => !open);
    }
}
