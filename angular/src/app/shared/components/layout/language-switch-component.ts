import { Component, computed, inject, Signal, signal } from '@angular/core';
import { _, TranslatePipe, TranslateService } from '@ngx-translate/core';
import { SupportedLangEnum } from '../../enums';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { LanguageService } from '../../services/translate-service';

@Component({
    selector: 'app-language-switcher',
    templateUrl: './language-switch-component.html',
    imports: [MatIconButton, MatIcon, MatMenu, MatMenuItem, MatMenuTrigger, TranslatePipe],
})
export class LanguageSwitcherComponent {
    private translateService = inject(TranslateService);
    private languageService = inject(LanguageService);

    /**
     * Aktuális nyelv
     */
    protected currentLanguage = signal(
        this.translateService.getCurrentLang() ?? this.translateService.getFallbackLang(),
    );

    /**
     * Nyelvek listája
     */
    protected languageData: Signal<{ id: string; name: string }[]> = computed(() => {
        return [
            {
                id: SupportedLangEnum.hu,
                name: this.translateService.instant(_('language.hu')),
            },
            {
                id: SupportedLangEnum.en,
                name: this.translateService.instant(_('language.en')),
            },
            {
                id: SupportedLangEnum.de,
                name: this.translateService.instant(_('language.de')),
            },
        ];
    });

    /**
     * Jelenlegi nyelv megváltoztatása
     */
    protected switchLanguage(lang: string): void {
        if (lang === this.translateService.currentLang()) {
            return;
        }

        this.languageService.setLanguage(lang);
        this.currentLanguage.set(lang);
    }
}
