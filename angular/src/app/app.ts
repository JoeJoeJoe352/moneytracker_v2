import { Component, HostListener, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './core/layout/header-component';
import { ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { SidebarComponent } from './core/layout/sidebar-component';
import { LayoutStore } from './core/layout/layout-store';

@Component({
    selector: 'app-root',
    templateUrl: './app.html',
    styleUrls: ['./app.scss', './core/layout/mobile-menu.scss'],
    imports: [RouterOutlet, HeaderComponent, ReactiveFormsModule, SidebarComponent, TranslatePipe],
})
export class App {
    protected readonly layoutStore = inject(LayoutStore);

    /**
     * Esc gomb listener, mi történjen, ha esc-t nyom a user
     */
    @HostListener('document:keydown.escape')
    protected onEsc() {
        this.layoutStore.closeMobileMenu();
    }
}
