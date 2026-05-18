package com.starbuck.moneytracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/register").permitAll()        // <-- Regisztráció végpont publikus
                .requestMatchers("/auth/login").permitAll()           // <-- Bejelentkezés végpont publikus
                .anyRequest().authenticated()                         // minden más védett
            );
            //.oauth2ResourceServer(oauth2 -> oauth2.jwt());            // TODO bekapcsolni a jwt alapú auth-ot, ha kész lesz

        return http.build();
    }
}
