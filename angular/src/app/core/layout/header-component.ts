import { Component, input, model } from '@angular/core';
import { MatToolbar } from '@angular/material/toolbar';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { LanguageSwitcherComponent } from './language-switch-component';
import { TranslatePipe } from '@ngx-translate/core';
import { RouterLink } from '@angular/router';

@Component({
    selector: 'app-header',
    templateUrl: './header-component.html',
    styleUrls: ['./header-component.scss', './mobile-menu.scss'],
    imports: [
        MatToolbar,
        MatIconButton,
        MatIcon,
        LanguageSwitcherComponent,
        TranslatePipe,
        RouterLink,
    ],
})
export class HeaderComponent {
    /**
     * Nyitva van-e a mobile menü
     */
    isMobileMenuOpen = model(false);

    /**
     * Rejtve van-e az oldalsáv (pl. landing oldal). Ilyenkor nincs hamburger gomb,
     * a logó pedig a headerben jelenik meg az oldalsáv helyett
     */
    isSidebarHidden = input(false);

    /**
     * hamburger menu lenyitása/bezárása
     */
    toggleMenu(): void {
        this.isMobileMenuOpen.update((open) => !open);
    }
}
