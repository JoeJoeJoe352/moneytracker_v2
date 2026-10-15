package com.starbuck.moneytracker.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.service.JwtService;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService("dGVzdC1vbmx5LWp3dC1zZWNyZXQtZm9yLXVuaXQtdGVzdHM=");

    private SecretKey signInKey;

    @BeforeEach
    void extractSignInKey() throws Exception {
        // A titkos kulcs privát, de a manipulált (lejárt/hamis aláírású) tokenek
        // előállításához szükségünk van rá - reflectionnel kérjük le, hogy ne
        // kelljen duplikálni a service-ben lévő konstanst
        Method getSignInKey = JwtService.class.getDeclaredMethod("getSignInKey");
        getSignInKey.setAccessible(true);
        this.signInKey = (SecretKey) getSignInKey.invoke(jwtService);
    }

    /**
     * A generált tokenből visszafejthető a beleírt email cím
     */
    @Test
    void extractEmail_returnsEmailFromGeneratedToken() {
        String token = jwtService.generateToken("teszt@email.com");

        assertEquals("teszt@email.com", jwtService.extractEmail(token));
    }

    /**
     * Két különböző usernek generált tokenje különböző
     */
    @Test
    void generateToken_producesDifferentTokensForDifferentUsers() {
        String token1 = jwtService.generateToken("user1@email.com");
        String token2 = jwtService.generateToken("user2@email.com");

        assertNotEquals(token1, token2);
    }

    /**
     * Érvényes, nem lejárt tokenre igaz, ha a user email címéhez lett generálva
     */
    @Test
    void isTokenValid_returnsTrueForMatchingUserAndValidToken() {
        User user = new User("testuser", "password", "teszt@email.com");
        String token = jwtService.generateToken("teszt@email.com");

        assertTrue(jwtService.isTokenValid(token, user));
    }

    /**
     * Ha a tokenben lévő email cím nem egyezik a user email címével, nem érvényes
     */
    @Test
    void isTokenValid_returnsFalseWhenEmailDoesNotMatch() {
        User otherUser = new User("otheruser", "password", "other@email.com");
        String token = jwtService.generateToken("teszt@email.com");

        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    /**
     * Lejárt tokenre a parse már a lejártság miatt hibát dob (a jjwt könyvtár
     * ezt már a parse-olás alatt észreveszi, nem csak utólag), így az
     * isTokenValid ExpiredJwtException-t dob, nem false-t ad vissza
     */
    @Test
    void isTokenValid_throwsExpiredJwtExceptionForExpiredToken() {
        User user = new User("testuser", "password", "teszt@email.com");
        String expiredToken = Jwts.builder()
                .subject("teszt@email.com")
                .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                .expiration(new Date(System.currentTimeMillis() - 5_000))
                .signWith(signInKey)
                .compact();

        assertThrows(ExpiredJwtException.class, () -> {
            jwtService.isTokenValid(expiredToken, user);
        });
    }

    /**
     * Ha a token aláírása nem egyezik (pl. más kulccsal lett aláírva, vagy
     * manipulálták), hibát dob
     */
    @Test
    void extractEmail_throwsForTamperedSignature() {
        String validToken = jwtService.generateToken("teszt@email.com");
        // utolsó karaktert kicseréljük a valid tokennek, így téve invaliddá
        String tamperedToken = validToken + "-forged";

        assertThrows(JwtException.class, () -> {
            jwtService.extractEmail(tamperedToken);
        });
    }

    /**
     * Nem valódi JWT formátumú stringre hibát dob
     */
    @Test
    void extractEmail_throwsForMalformedToken() {
        assertThrows(JwtException.class, () -> {
            jwtService.extractEmail("not.a.jwt");
        });
    }
}
