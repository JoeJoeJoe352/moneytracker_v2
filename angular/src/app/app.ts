import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Header } from './shared/components/header';
import { Footer } from './shared/components/footer';
import { ReactiveFormsModule } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { SUPPORTED_LANGS } from './shared/utils/language-util';
import { SupportedLangEnum } from './shared/enums';

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
            fallBackLang = SupportedLangEnum.en;
        }

        const lang = localStorage.getItem('lang') ?? fallBackLang;
        this.translate.use(lang);
    }
}
