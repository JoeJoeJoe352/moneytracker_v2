import { Component, HostListener } from '@angular/core';
import { MatToolbar } from '@angular/material/toolbar';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { LanguageSwitcherComponent } from './language-switch-component';

@Component({
    selector: 'app-header',
    templateUrl: 'header.html',
    styleUrl: './header.scss',
    imports: [MatToolbar, MatIconButton, MatIcon, LanguageSwitcherComponent],
})
export class Header {
    /**
     * Hamburger menu lenyitása
     */
    protected menuOpen = false;

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
}
