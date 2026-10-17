package com.starbuck.moneytracker.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    /**
     * Az alkalmazás "most"-ja. Beanként injektálva, hogy a tesztek fix időpontot adhassanak meg
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
