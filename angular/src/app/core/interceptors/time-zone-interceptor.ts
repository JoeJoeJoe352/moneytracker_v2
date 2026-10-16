import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Beállítja a requestek időzónáját
 */
export const TimeZoneInterceptor: HttpInterceptorFn = (req, next) => {
    const modified = req.clone({
        setHeaders: {
            'x-time-zone': getTimeZone(),
        },
    });

    return next(modified);
};

function getTimeZone(): string {
    return Intl.DateTimeFormat().resolvedOptions().timeZone;
}
