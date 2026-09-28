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

    const isLoadedFromDb = await userDataStore.loadUserFromDb().catch((error) => {
        console.error('failed to read user data from local db', error);
        return false;
    });

    // Mindenképpen újra lekérjük a user adatokat, (hátha változtak az előző óta). Ezek a háttérben fognak frissülni
    const authCheck = initUserData(authService, userDataStore, isLoadedFromDb);
    if (!isLoadedFromDb) {
        await authCheck;
    }
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
    isLoadedFromDb: boolean,
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

        // egyéb hibánál (pl. offline) a lokálisan betöltött adatok maradnak
        if (isAuthError || !isLoadedFromDb) {
            userDataStore.resetData();
        }
    }
}
