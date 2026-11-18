package com.fraud.backend.dto;

import com.fraud.backend.entity.User;

public record UserResponse(Long id, String name, String email, User.Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
