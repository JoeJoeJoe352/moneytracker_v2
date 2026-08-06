import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { initApp } from './app.initializer';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { credentialsInterceptor } from './interceptor';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { AuthService } from './features/auth/auth-service';
import { UserDataStore } from './shared/services/user-data-store';

export const appConfig: ApplicationConfig = {
    providers: [
        provideBrowserGlobalErrorListeners(),
        provideRouter(routes),
        provideHttpClient(
            // emiatt minden kérés withcredentials-al fut le
            withInterceptors([credentialsInterceptor]),
        ),
        provideTranslateService({
            loader: provideTranslateHttpLoader({
                prefix: './i18n/',
                suffix: '.json',
            }),
            fallbackLang: 'hu',
            lang: 'hu',
        }),
        provideAppInitializer(() => initApp(inject(AuthService), inject(UserDataStore))),
    ],
};
