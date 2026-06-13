package com.starbuck.moneytracker.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.starbuck.moneytracker.entity.User;

@Component
public class CurrentUserUtil {

    /**
     * Visszatér a requestet küldő user objektummal
     * 
     * @return
     */
    public User getUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? (User) auth.getPrincipal() : null;
    }

    /**
     * Visszatér a requestet küldő user nevével
     * 
     * @return String
     */
    public String getUsername() {
        User user = this.getUser();
        return user != null ? user.getUsername() : null;
    }
}
