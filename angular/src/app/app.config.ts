import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { initApp } from './app.initializer';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { CredentialsInterceptor } from './interceptor';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { AuthService } from './features/auth/auth-service';
import { UserDataStore } from './shared/services/user-data-store';
import { LanguageInterceptor } from './language-interceptor';
import { LANGUAGE_EN } from './shared/components/language-switch-component';

export const appConfig: ApplicationConfig = {
    providers: [
        provideBrowserGlobalErrorListeners(),
        provideRouter(routes),
        provideHttpClient(
            withInterceptors([
                CredentialsInterceptor, 
                LanguageInterceptor
            ]),
        ),
        provideTranslateService({
            loader: provideTranslateHttpLoader({
                prefix: './i18n/',
                suffix: '.json',
            }),
            fallbackLang: LANGUAGE_EN,
        }),
        provideAppInitializer(() => initApp(inject(AuthService), inject(UserDataStore))),
    ],
};
