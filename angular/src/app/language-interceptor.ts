import { HttpInterceptorFn } from '@angular/common/http';
import { SupportedLangEnum } from './shared/enums';

/**
 * Beállítja az oldal nyelvét
 */
export const LanguageInterceptor: HttpInterceptorFn = (req, next) => {
    let lang = localStorage.getItem('lang'); // lehet a translateService még nincs betöltve itt

    if (lang === null) {
        console.error('Lang is not set in localStorage when the interceptor activated');
        lang = SupportedLangEnum.en;
        localStorage.setItem('lang', lang);
    }

    const modified = req.clone({
        setHeaders: {
            'Accept-Language': lang,
        },
    });

    return next(modified);
};
