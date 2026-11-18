package com.fraud.backend.service;

import com.fraud.backend.dto.RegisterRequest;
import com.fraud.backend.dto.UserResponse;
import com.fraud.backend.entity.User;
import com.fraud.backend.exception.EmailAlreadyRegisteredException;
import com.fraud.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserRegistrationServiceTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserRegistrationService service =
            new UserRegistrationService(userRepository, passwordEncoder);

    @Test
    void registerNormalizesEmailHashesPasswordAndAssignsAnalystRole() {
        RegisterRequest request = new RegisterRequest("  Alex Smith ", " Alex@Example.com ", "password123");
        when(userRepository.existsByEmailIgnoreCase("alex@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = service.register(request);

        assertEquals("Alex Smith", response.name());
        assertEquals("alex@example.com", response.email());
        assertEquals(User.Role.ANALYST, response.role());
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerRejectsAnExistingEmailWithoutSaving() {
        RegisterRequest request = new RegisterRequest("Alex Smith", "Alex@Example.com", "password123");
        when(userRepository.existsByEmailIgnoreCase("alex@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class, () -> service.register(request));

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }
}
