package com.starbuck.moneytracker.config;

import java.io.IOException;
import java.util.Arrays;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.starbuck.moneytracker.controller.AuthController;
import com.starbuck.moneytracker.service.JwtService;
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
    private final UserDetailsService userDetailsService; 

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Sütikből megkeressük a JWT sütit
        final Cookie[] cookies = request.getCookies();
        String jwt = Arrays.stream(cookies)
            .filter(cookie -> AuthController.AUTH_COOKIE_NAME.equals(cookie.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);

        if (jwt == null) {
            // Ha nincs authentikációs süti, akkor továbbengedi a requestet. A nem kivételes végpontoknál így nem lesz hiba
            // A többi végpontnál viszont ettől még az lesz
            filterChain.doFilter(request, response);
            return; 
        }

        final String username = jwtService.extractUsername(jwt);

        // Nem voltunk még bejelentkezve ebben a requestben, de van username a tokenben
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(jwt, userDetails)) {
                // Security context feltöltése a userdetails alapjánuser
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken (
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
