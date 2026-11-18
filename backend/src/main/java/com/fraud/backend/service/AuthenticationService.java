package com.fraud.backend.service;

import com.fraud.backend.dto.AuthResponse;
import com.fraud.backend.dto.LoginRequest;
import com.fraud.backend.dto.UserResponse;
import com.fraud.backend.exception.InvalidCredentialsException;
import com.fraud.backend.repository.UserRepository;
import com.fraud.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        var user = userRepository.findByEmailIgnoreCase(email)
                .filter(account -> passwordEncoder.matches(request.password(), account.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        return new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpirationSeconds(),
                UserResponse.from(user)
        );
    }
}
