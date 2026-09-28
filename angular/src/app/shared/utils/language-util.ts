import { Injectable } from '@angular/core';
import { SupportedLangEnum } from '../enums';

/**
 * Az alkalmazás jelenleg támogatott nyelvei
 */
export const SUPPORTED_LANGS = [SupportedLangEnum.hu, SupportedLangEnum.de, SupportedLangEnum.en];

/**
 * Nyelv enum => nyelv string mappelése
 */
export const LANGUAGE_TO_LOCALE: Record<string, string> = {
    [SupportedLangEnum.hu]: 'hu',
    [SupportedLangEnum.en]: 'en',
    [SupportedLangEnum.de]: 'de',
};

/**
 * A nyelvhez tartozó locale, ismeretlen vagy hiányzó nyelvnél az angol
 */
export function getLocaleForLang(lang: string): string {
    return (lang && LANGUAGE_TO_LOCALE[lang]) ?? 'en';
}

@Injectable({
    providedIn: 'root',
})
export class LanguageUtil {}
