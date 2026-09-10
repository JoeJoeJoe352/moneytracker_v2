import { inject, Injectable } from '@angular/core';
import { DateAdapter } from '@angular/material/core';
import { TranslateService } from '@ngx-translate/core';

export const LOCALSTORAGE_KEY_LANG = 'lang';

@Injectable({
    providedIn: 'root',
})
export class LanguageService {
    private translateService = inject(TranslateService);
    private dateAdapter = inject(DateAdapter);

    /**
     * Oldal nyelvének beállítása
     */
    public setLanguage(language: string): void {
        localStorage.setItem(LOCALSTORAGE_KEY_LANG, language);
        this.translateService.use(language);
        // dátum és pénzek nyelvesített megjelenítése
        this.dateAdapter.setLocale(language);
    }
}
