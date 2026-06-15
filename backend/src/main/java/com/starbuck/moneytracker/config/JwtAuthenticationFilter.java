package com.starbuck.moneytracker.config;

import java.io.IOException;
import java.util.Arrays;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.starbuck.moneytracker.controller.AuthController;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.service.CustomUserDetailService;
import com.starbuck.moneytracker.service.JwtService;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Requestenként egyszer lefutó filter
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter{

    /** Token validálása + username kiolvasás */
    private final JwtService jwtService;
    private final CustomUserDetailService userService;

    public JwtAuthenticationFilter(
        JwtService jwtService, 
        CustomUserDetailService userService
    ) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Sütikből megkeressük a JWT sütit
        final Cookie[] cookies = request.getCookies();
        String jwt = null;
        // Pl.: postmanben a cookies az üres
        if (cookies != null) {
            jwt = Arrays.stream(cookies)
                .filter(cookie -> AuthController.AUTH_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
            }
        if (jwt == null) {
            // Ha nincs authentikációs süti, akkor továbbengedi a requestet. A nem kivételes végpontoknál így nem lesz hiba
            // A többi végpontnál viszont ettől még az lesz
            filterChain.doFilter(request, response);
            return; 
        }

        String username = null;
        try {
            username = jwtService.extractUsername(jwt);
        } catch(ExpiredJwtException exception) {
            // lejárt a token, nem gond, majd bejelentkezik a user újból
            filterChain.doFilter(request, response);
            return;
        }

        // Nem voltunk még bejelentkezve ebben a requestben, de van username a tokenben
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            User user = this.userService.loadUserByUsername(username);

            if (jwtService.isTokenValid(jwt, user)) {
                // Security context feltöltése a userdetails alapján
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken (
                        user,
                        null,
                        user.getAuthorities()
                    );
                // beállítja a securitycontextbe a user adatokat
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
