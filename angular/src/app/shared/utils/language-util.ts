import { Injectable } from "@angular/core";

export const LANGUAGE_HU = 'hu';
export const LANGUAGE_EN = 'en';
export const LANGUAGE_DE = 'de';

export const SUPPORTED_LANGS = [LANGUAGE_HU, LANGUAGE_DE, LANGUAGE_EN]

export const LANGUAGE_TO_LOCALE: Record<string, string> = {
    [LANGUAGE_HU]: 'hu',
    [LANGUAGE_EN]: 'en',
    [LANGUAGE_DE]: 'de',
};
@Injectable({
    providedIn: 'root',
})
export class LanguageUtil {

}