import { HttpInterceptorFn } from '@angular/common/http';
import { LOCALSTORAGE_KEY_LANG } from './shared/services/translate-service';

/**
 * Beállítja a requestek nyelvi headerjét
 */
export const LanguageInterceptor: HttpInterceptorFn = (req, next) => {
    const lang = localStorage.getItem(LOCALSTORAGE_KEY_LANG); // lehet a translateService még nincs betöltve itt

    if (lang === null) {
        // Ez elvileg nem történhet meg, ezért nem dolgunk itt a nyelvet beállítani
        throw new Error('Lang is not set in localStorage when the interceptor activated');
    }

    const modified = req.clone({
        setHeaders: {
            'Accept-Language': lang,
        },
    });

    return next(modified);
};
