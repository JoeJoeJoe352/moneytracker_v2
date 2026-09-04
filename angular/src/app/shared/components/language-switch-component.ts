import { Component, computed, inject, Signal, signal } from '@angular/core';
import { _, TranslateService } from '@ngx-translate/core';
import { FormsModule } from '@angular/forms';
import { LANGUAGE_DE, LANGUAGE_EN, LANGUAGE_HU } from '../utils/language-util';

@Component({
    selector: 'app-language-switcher',
    standalone: true,
    template: `
        <select class="form-control" (change)="switchLanguage()" [(ngModel)]="currentLanguage">
            @for (language of languageData(); track $index) {
                <option [value]="language.id">
                    {{ language.name }}
                </option>
            }
        </select>
    `,

    imports: [FormsModule],
})
export class LanguageSwitcherComponent {
    private translateService = inject(TranslateService);

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
                id: LANGUAGE_HU,
                name: this.translateService.instant(_('language.hu')),
            },
            {
                id: LANGUAGE_EN,
                name: this.translateService.instant(_('language.en')),
            },
            {
                id: LANGUAGE_DE,
                name: this.translateService.instant(_('language.de')),
            },
        ];
    });
    /**
     * Jelenlegi nyelv megváltoztatása
     */
    protected switchLanguage(): void {
        const currentLangString = this.currentLanguage();
        if (!currentLangString) {
            throw new Error('Lang is not set! ' + currentLangString);
        }
        if (currentLangString === this.translateService.currentLang()) {
            return;
        }

        localStorage.setItem('lang', currentLangString);
        this.translateService.use(currentLangString);
    }
}
