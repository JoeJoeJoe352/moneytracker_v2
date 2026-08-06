package com.starbuck.moneytracker.util;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    /**
     * Authentikációs cookie neve
     */
    public static final String AUTH_COOKIE_NAME = "AUTH_TOKEN";

    /**
     * Jwt token lejárati ideje (7 nap jelenleg)
     */
    public static final int COOKIE_MAX_LIFETIME_SECONDS = 7 * 24 * 60 * 60;

    /**
     * Visszaad egy ResponseCookie objektumot, ami a JWT tokenből készül
     * 
     * @param jwtToken
     * @return
     */
    public ResponseCookie getJwtCookie(String jwtToken) {
        ResponseCookie cookie = ResponseCookie.from(AUTH_COOKIE_NAME, jwtToken)
                .httpOnly(true)
                .secure(true) // csak HTTPS-en keresztül küldhető
                .path("/") // Az egész oldal a szkópja a sütinek
                .maxAge(COOKIE_MAX_LIFETIME_SECONDS) // 7 nap
                .sameSite("Strict") // CSRF védelem
                .build();
        return cookie;
    }

    /**
     * Session törlő cookie elkészítése
     * Ez egy azonnal lejáró süti, ami a böngészőből törli a meglévő sütit
     * 
     * @return
     */
    public ResponseCookie getJwtSessionDestroyCookie() {
        ResponseCookie expiredCookie = ResponseCookie.from(AUTH_COOKIE_NAME, "")
                .path("/")
                .httpOnly(true)
                .secure(true)
                .maxAge(0)
                .sameSite("Strict")
                .build();
        return expiredCookie;
    }

}
