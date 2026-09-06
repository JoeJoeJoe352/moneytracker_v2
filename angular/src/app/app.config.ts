import {
    ApplicationConfig,
    inject,
    provideAppInitializer,
    provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideNativeDateAdapter } from '@angular/material/core';

import { routes } from './app.routes';
import { initApp } from './app.initializer';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { CredentialsInterceptor } from './interceptor';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { AuthService } from './features/auth/auth-service';
import { UserDataStore } from './shared/services/user-data-store';
import { LanguageInterceptor } from './language-interceptor';
import { SupportedLangEnum } from './shared/enums';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';

export const appConfig: ApplicationConfig = {
    providers: [
        provideBrowserGlobalErrorListeners(),
        provideRouter(routes),
        provideNativeDateAdapter(),
        {
            provide: MAT_FORM_FIELD_DEFAULT_OPTIONS,
            useValue: { appearance: 'outline' },
        },
        provideHttpClient(withInterceptors([CredentialsInterceptor, LanguageInterceptor])),
        provideTranslateService({
            loader: provideTranslateHttpLoader({
                prefix: './i18n/',
                suffix: '.json',
            }),
            fallbackLang: SupportedLangEnum.en,
        }),
        provideAppInitializer(() => initApp(inject(AuthService), inject(UserDataStore))),
    ],
};
