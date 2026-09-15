import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { LOCALSTORAGE_KEY_LANG } from './app/shared/services/translate-service';
import { SupportedLangEnum } from './app/shared/enums';

// legelső betöltéskor nincs a localstorage-be a nyelv beállítva, de a LanguageInterceptornak kellene, hogy a fordítási file-t be tudja tölteni
if (!localStorage.getItem(LOCALSTORAGE_KEY_LANG)) {
    localStorage.setItem(LOCALSTORAGE_KEY_LANG, SupportedLangEnum.en);
}

bootstrapApplication(App, appConfig).catch((err) => console.error(err));
