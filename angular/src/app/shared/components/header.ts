import { Component, input, output } from '@angular/core';
import { MatToolbar } from '@angular/material/toolbar';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { LanguageSwitcherComponent } from './language-switch-component';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'app-header',
    templateUrl: 'header.html',
    styleUrls: ['./header.scss', './mobile-menu.scss'],
    imports: [MatToolbar, MatIconButton, MatIcon, LanguageSwitcherComponent, TranslatePipe],
})
export class Header {
    /**
     * Nyitva van-e a mobile menü
     */
    isMobileMenuOpen = input(false)

    /**
     * Mobil menüt átkapcsolta a user
     */
    mobileMenuToggled = output<void>()

    /**
     * hamburger menu lenyitása/bezárása
     */
    toggleMenu(): void {
        this.mobileMenuToggled.emit();
    }
}
