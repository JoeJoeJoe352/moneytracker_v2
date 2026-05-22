package com.starbuck.moneytracker.service;

import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final String SECRET_KEY = "aV9hbV9wZXRlcl8yMDI2MDUyMl9tb25leXRyYWNrZXJfYXBwXzE=";

    /**
     * A tokenben lévő Subject mezőből kinyert username-el tér vissza
     * 
     * @param String token
     * @return String A felhasználó neve
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Token generálása a username alapján. A tokenben a username lesz a subject, 1 napos érvényességgel.
     * 
     * @param String username
     * @return String A generált token
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username) // subject mező
                .issuedAt(new Date()) // mikor generálódott
                .expiration(new Date(System.currentTimeMillis() + 24 * 1000 * 60 * 60)) // lejárati idő
                .signWith(getSignInKey()) // aláírás a titkos kulccsal
                .compact();
    }

    /**
     * Token validitás ellenőrzése: a tokenben lévő username megegyezik-e a userdetails username-jével, és a token nincs-e lejárva
     * 
     * @param token
     * @param userDetails
     * @return
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Lejárt-e már a token
     * 
     * @param token
     * @return
     */
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /**
     * Tokent ellenőrzi, hogy helyes-e, lett-e módosítva, stb
     * 
     * @param token
     * @return
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * A titkos kulcs dekódolása és SecretKey objektummá alakítása, amit a token aláírásához és ellenőrzéséhez használunk
     * 
     * @return SecretKey
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

