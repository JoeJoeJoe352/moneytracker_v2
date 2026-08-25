import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Beállítja az oldal nyelvét
 */
export const LanguageInterceptor: HttpInterceptorFn = (req, next) => {
    const lang = localStorage.getItem('lang') || "en";

    const modified = req.clone({
        setHeaders: {
            'Accept-Language': lang,
        },
    });

    return next(modified);
};