import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Header } from './shared/components/header';
import { ReactiveFormsModule } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { SUPPORTED_LANGS } from './shared/utils/language-util';
import { SupportedLangEnum } from './shared/enums';
import { LanguageService, LOCALSTORAGE_KEY_LANG } from './shared/services/translate-service';
import { Sidebar } from './shared/components/sidebar';

@Component({
    selector: 'app-root',
    templateUrl: './app.html',
    styleUrl: './app.scss',
    imports: [RouterOutlet, Header, ReactiveFormsModule, Sidebar],
})
export class App {
    protected readonly title = signal('Moneytracker');
    private translate = inject(TranslateService);
    private languageService = inject(LanguageService);

    constructor() {
        this.translate.addLangs(SUPPORTED_LANGS);

        let fallBackLang = this.translate.getFallbackLang();
        if (fallBackLang === null) {
            console.error('Please set the fallback lang');
            fallBackLang = SupportedLangEnum.en;
        }

        const lang = localStorage.getItem(LOCALSTORAGE_KEY_LANG) ?? fallBackLang;
        this.languageService.setLanguage(lang);
    }
}
