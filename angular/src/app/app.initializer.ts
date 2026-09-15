import { AuthService } from './features/auth/auth-service';
import { LanguageService } from './shared/services/translate-service';
import { UserDataStore } from './shared/services/user-data-store';
import { SUPPORTED_LANGS } from './shared/utils/language-util';

export function initApp(
    authService: AuthService,
    userDataStore: UserDataStore,
    languageService: LanguageService,
) {
    return new Promise<void>((resolve) => {
        languageService.registerSupportedLangs(SUPPORTED_LANGS);

        // ha még nincs elmentett nyelv (első oldalbetöltés), a fallback nyelvet használjuk
        const lang = languageService.getLanguageFromLocalStore() ?? languageService.getFallbackLang();
        languageService.setLanguage(lang);

        authService.authenticateUser().subscribe({
            next: (userData) => {
                userDataStore.loadUserData(userData);
                resolve();
            },
            error: (error) => {
                userDataStore.resetData();
                // 403 a válasz, ha nem jó a jwt token
                if (error.status !== 403) {
                    console.error('unknown error during authcheck!', error);
                }
                resolve();
            },
        });
    });
}
