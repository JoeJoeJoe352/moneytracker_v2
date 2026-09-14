import { Component, EventEmitter, Output } from '@angular/core';
import { MatToolbar } from '@angular/material/toolbar';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { LanguageSwitcherComponent } from './language-switch-component';

@Component({
    selector: 'app-header',
    templateUrl: 'header.html',
    styleUrls: ['./header.scss', './mobile-menu.scss'],
    imports: [MatToolbar, MatIconButton, MatIcon, LanguageSwitcherComponent],
})
export class Header {
    @Output() mobileMenuToggled = new EventEmitter<void>();

    /**
     * hamburger menu lenyitása/bezárása
     */
    toggleMenu(): void {
        this.mobileMenuToggled.emit();
    }
}
