import { Injectable } from "@angular/core";
import { SupportedLangEnum } from "../enums";

export const SUPPORTED_LANGS = [SupportedLangEnum.hu, SupportedLangEnum.de, SupportedLangEnum.en]

export const LANGUAGE_TO_LOCALE: Record<string, string> = {
    [SupportedLangEnum.hu]: 'hu',
    [SupportedLangEnum.en]: 'en',
    [SupportedLangEnum.de]: 'de',
};
@Injectable({
    providedIn: 'root',
})
export class LanguageUtil {

}