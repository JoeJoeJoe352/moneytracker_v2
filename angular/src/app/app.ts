import { Component, HostListener, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { Header } from './shared/components/header';
import { ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { SUPPORTED_LANGS } from './shared/utils/language-util';
import { SupportedLangEnum } from './shared/enums';
import { LanguageService, LOCALSTORAGE_KEY_LANG } from './shared/services/translate-service';
import { Sidebar } from './shared/components/sidebar';

@Component({
    selector: 'app-root',
    templateUrl: './app.html',
    styleUrls: ['./app.scss', './shared/components/mobile-menu.scss'],
    imports: [RouterOutlet, Header, ReactiveFormsModule, Sidebar, TranslatePipe],
})
export class App {
    protected readonly title = signal('Moneytracker');
    private translate = inject(TranslateService);
    private languageService = inject(LanguageService);
    private router = inject(Router);

    protected isMobileMenuOpen = signal(false);

    constructor() {
        this.translate.addLangs(SUPPORTED_LANGS);

        let fallBackLang = this.translate.getFallbackLang();
        if (fallBackLang === null) {
            console.error('Please set the fallback lang');
            fallBackLang = SupportedLangEnum.en;
        }

        const lang = localStorage.getItem(LOCALSTORAGE_KEY_LANG) ?? fallBackLang;
        this.languageService.setLanguage(lang);

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
