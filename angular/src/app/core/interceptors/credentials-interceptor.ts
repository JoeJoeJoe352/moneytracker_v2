import { HttpInterceptorFn } from '@angular/common/http';

const API_URL_PREFIX = '/api/';

/**
 * Beállítja, hogy küldje-e a frontend a auth cookie-t a kéréskor
 */
export const CredentialsInterceptor: HttpInterceptorFn = (req, next) => {
    const isApiUrl = req.url.startsWith(API_URL_PREFIX);
    if (!isApiUrl) {
        return next(req);
    }

    const modified = req.clone({
        withCredentials: true,
    });

    return next(modified);
};
