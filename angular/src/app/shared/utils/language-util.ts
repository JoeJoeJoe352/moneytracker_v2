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

@Injectable({
    providedIn: 'root',
})
export class LanguageUtil {}
