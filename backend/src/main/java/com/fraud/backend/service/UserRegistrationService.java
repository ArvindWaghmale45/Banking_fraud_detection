package com.fraud.backend.service;

import com.fraud.backend.dto.RegisterRequest;
import com.fraud.backend.dto.UserResponse;
import com.fraud.backend.entity.User;
import com.fraud.backend.exception.EmailAlreadyRegisteredException;
import com.fraud.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                User.Role.ANALYST
        );

        return UserResponse.from(userRepository.save(user));
    }
}
