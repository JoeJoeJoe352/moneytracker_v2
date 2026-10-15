package com.starbuck.moneytracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// A usert a JwtAuthenticationFilter tölti be, ezért nincs szükség a Spring alapértelmezett, generált jelszavas userére
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class MoneytrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoneytrackerApplication.class, args);
	}

}
