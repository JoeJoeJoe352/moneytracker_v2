import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { AuthService } from './features/auth/auth-service';
import { appInitializerProvider } from './app.initializer';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { credentialsInterceptor } from './interceptor';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';

export const appConfig: ApplicationConfig = {
    providers: [
        provideBrowserGlobalErrorListeners(),
        provideRouter(routes),
        provideHttpClient(
            // emiatt minden kérés withcredentials-al fut le
            withInterceptors([credentialsInterceptor]),
        ),
        appInitializerProvider, // emiatt lefut az auth ellenörzés, mielőtt elindul a program
        provideHttpClient(), // nyelvesítés
        provideTranslateService({
            loader: provideTranslateHttpLoader({
                prefix: './i18n/',
                suffix: '.json',
            }),
            fallbackLang: 'hu',
            lang: 'hu',
        }),
    ],
};

export function initApp(authService: AuthService) {
    return () => authService.loadUser();
}
