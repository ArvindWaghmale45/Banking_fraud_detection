package com.fraud.backend.service;

import com.fraud.backend.dto.LoginRequest;
import com.fraud.backend.entity.User;
import com.fraud.backend.exception.InvalidCredentialsException;
import com.fraud.backend.repository.UserRepository;
import com.fraud.backend.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationServiceTests {

    private static final String TEST_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = new JwtService(TEST_SECRET, 3600);
    private final AuthenticationService service =
            new AuthenticationService(userRepository, passwordEncoder, jwtService);

    @Test
    void loginReturnsSignedTokenAndSafeUserDetails() {
        User user = new User("Alex Smith", "alex@example.com", "$2a$hash", User.Role.ANALYST);
        when(userRepository.findByEmailIgnoreCase("alex@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$hash")).thenReturn(true);

        var response = service.login(new LoginRequest(" Alex@Example.com ", "password123"));

        Claims claims = jwtService.parseToken(response.accessToken());
        assertEquals("alex@example.com", claims.getSubject());
        assertEquals("ANALYST", claims.get("role"));
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600, response.expiresIn());
        assertEquals("alex@example.com", response.user().email());
        verify(passwordEncoder).matches("password123", "$2a$hash");
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.login(new LoginRequest("unknown@example.com", "password123"))
        );
    }

    @Test
    void loginRejectsIncorrectPassword() {
        User user = new User("Alex Smith", "alex@example.com", "$2a$hash", User.Role.ANALYST);
        when(userRepository.findByEmailIgnoreCase("alex@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "$2a$hash")).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.login(new LoginRequest("alex@example.com", "wrong-password"))
        );
    }
}
