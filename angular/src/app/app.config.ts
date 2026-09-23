import {
    ApplicationConfig,
    inject,
    isDevMode,
    provideAppInitializer,
    provideBrowserGlobalErrorListeners,
    provideZonelessChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideNativeDateAdapter } from '@angular/material/core';
import { Overlay } from '@angular/cdk/overlay';
import { MAT_DIALOG_SCROLL_STRATEGY } from '@angular/material/dialog';

import { routes } from './app.routes';
import { initApp } from './app.initializer';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { CredentialsInterceptor } from './credentials-interceptor';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { AuthService } from './features/auth/auth-service';
import { UserDataStore } from './shared/stores/user-data-store';
import { LanguageInterceptor } from './language-interceptor';
import { SupportedLangEnum } from './shared/enums';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { MAT_PROGRESS_SPINNER_DEFAULT_OPTIONS } from '@angular/material/progress-spinner';
import { MAT_SNACK_BAR_DEFAULT_OPTIONS } from '@angular/material/snack-bar';
import { provideServiceWorker } from '@angular/service-worker';
import { LanguageService } from './shared/services/translate-service';

export const appConfig: ApplicationConfig = {
    providers: [
        provideBrowserGlobalErrorListeners(),
        provideZonelessChangeDetection(),
        provideRouter(routes),
        provideNativeDateAdapter(),
        // A dialog megnyitásakor ne fagyassza be a html-t (position: fixed), mert az levágja
        // a viewporton túli tartalmat, ha a lap le van görgetve. A noop() miatt a háttér
        // görgetési pozíciója egyszerűen nem változik, amíg a dialog nyitva van.
        {
            provide: MAT_DIALOG_SCROLL_STRATEGY,
            useFactory: (overlay: Overlay) => () => overlay.scrollStrategies.noop(),
            deps: [Overlay],
        },
        {
            provide: MAT_FORM_FIELD_DEFAULT_OPTIONS,
            useValue: { appearance: 'outline' },
        },
        {
            provide: MAT_PROGRESS_SPINNER_DEFAULT_OPTIONS,
            useValue: { diameter: 50 },
        },
        {
            provide: MAT_SNACK_BAR_DEFAULT_OPTIONS,
            useValue: { duration: 2500 },
        },
        provideHttpClient(withInterceptors([CredentialsInterceptor, LanguageInterceptor])),
        provideTranslateService({
            loader: provideTranslateHttpLoader({
                prefix: './i18n/',
                suffix: '.json',
            }),
            fallbackLang: SupportedLangEnum.en,
        }),
        provideAppInitializer(() => initApp(inject(AuthService), inject(UserDataStore), inject(LanguageService))),
        provideServiceWorker('ngsw-worker.js', {
            enabled: !isDevMode(),
            registrationStrategy: 'registerWhenStable:30000',
        }),
    ],
};
