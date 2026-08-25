package com.starbuck.moneytracker.util;

import java.util.Locale;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.entity.enum_entites.LangEnum;

@Component
public class LocaleToLangEnumConverter implements Converter<Locale, LangEnum> {

    @Override
    public LangEnum convert(Locale locale) {
        if (locale == null) {
            return LangEnum.EN; // fallback
        }

        return switch (locale.getLanguage()) {
            case "hu" -> LangEnum.HU;
            case "de" -> LangEnum.DE;
            default -> LangEnum.EN;
        };
    }
}