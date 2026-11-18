package com.fraud.backend.controller;

import com.fraud.backend.dto.RegisterRequest;
import com.fraud.backend.dto.AuthResponse;
import com.fraud.backend.dto.LoginRequest;
import com.fraud.backend.dto.UserResponse;
import com.fraud.backend.service.AuthenticationService;
import com.fraud.backend.service.UserRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserRegistrationService userRegistrationService;
    private final AuthenticationService authenticationService;

    public AuthController(
            UserRegistrationService userRegistrationService,
            AuthenticationService authenticationService
    ) {
        this.userRegistrationService = userRegistrationService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return userRegistrationService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }
}
