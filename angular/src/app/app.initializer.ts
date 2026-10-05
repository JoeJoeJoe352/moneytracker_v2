import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './features/auth/auth-service';
import { LanguageService } from './shared/services/translate-service';
import { UserDataStore } from './shared/stores/user-data-store';
import { SUPPORTED_LANGS } from './shared/utils/language-util';

export async function initApp(
    authService: AuthService,
    userDataStore: InstanceType<typeof UserDataStore>,
    languageService: LanguageService,
): Promise<void> {
    setLang(languageService);
    await initUserData(authService, userDataStore);
}

/**
 * Nyelvi adatok beállítása
 */
function setLang(languageService: LanguageService) {
    languageService.registerSupportedLangs(SUPPORTED_LANGS);

    // ha még nincs elmentett nyelv (első oldalbetöltés), a fallback nyelvet használjuk
    const lang = languageService.getLanguageFromLocalStore() ?? languageService.getFallbackLang();
    languageService.setLanguage(lang);
}

/**
 * User adatok lekérése a szerverről és beállítása a store-ba
 */
async function initUserData(
    authService: AuthService,
    userDataStore: InstanceType<typeof UserDataStore>,
): Promise<void> {
    try {
        userDataStore.loadUserData(await firstValueFrom(authService.authenticateUser()));
    } catch (error) {
        // TODO legyen 401, ha üres vagy lejárt a token
        // 403 a válasz, ha nem jó a jwt token
        const isAuthError =
            error instanceof HttpErrorResponse && (error.status === 401 || error.status === 403);

        if (!isAuthError) {
            console.error('unknown error during authcheck!', error);
        }

        userDataStore.resetData();
    }
}
