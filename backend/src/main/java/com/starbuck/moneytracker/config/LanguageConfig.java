package com.starbuck.moneytracker.config;

import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;

@Configuration
public class LanguageConfig {

    @Bean
    public LocaleResolver localeResolver() {
        TimeZoneAwareLocaleResolver resolver = new TimeZoneAwareLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setSupportedLocales(List.of(
                Locale.ENGLISH,
                Locale.GERMAN,
                Locale.of("hu")));
        return resolver;
    }
}
