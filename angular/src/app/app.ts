import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Header } from './shared/components/header';
import { Footer } from './shared/components/footer';
import { ReactiveFormsModule } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { LANGUAGE_EN, SUPPORTED_LANGS } from './shared/utils/language-util';

@Component({
    selector: 'app-root',
    imports: [RouterOutlet, Header, Footer, ReactiveFormsModule],
    templateUrl: './app.html',
    styleUrl: './app.scss',
})
export class App {
    protected readonly title = signal('Moneytracker');
    private translate = inject(TranslateService);

    constructor() {
        this.translate.addLangs(SUPPORTED_LANGS);

        let fallBackLang = this.translate.getFallbackLang();
        if (fallBackLang === null) {
            console.error('Please set the fallback lang');
            fallBackLang = LANGUAGE_EN;
        }

        const lang = localStorage.getItem('lang') ?? fallBackLang;
        this.translate.use(lang);
    }
}
