package com.starbuck.moneytracker.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.repository.UserRepository;
import com.starbuck.moneytracker.service.CustomUserDetailService;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailService customUserDetailService;

    /**
     * Ha létezik a userrel a felhasználónév, azt adja vissza
     */
    @Test
    void loadUserByUsername_returnsUserWhenFound() {
        User userInDB = new User("testuser", "encodedPassword", "teszt@email.com");
        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(userInDB);

        User result = customUserDetailService.loadUserByUsername("testuser");

        assertEquals(userInDB, result);
    }

    /**
     * Ha nem található a felhasználónévvel user, UsernameNotFoundException-t
     * dob
     */
    @Test
    void loadUserByUsername_throwsWhenUserNotFound() {
        Mockito.when(userRepository.findByUsername("missing")).thenReturn(null);

        assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailService.loadUserByUsername("missing");
        });
    }
}
