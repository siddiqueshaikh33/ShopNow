package com.example.demo.Dto;

import java.time.LocalDateTime;

import com.example.demo.Entity.Provider;
import com.example.demo.Entity.Role;
import com.example.demo.Entity.Users;

public record UserResponseDto(
    int userId,
    String username,
    String email,
    Role role,
    Provider provider,
    LocalDateTime createdAt
) {
    public UserResponseDto(int userId, String username, String email, Role role, LocalDateTime createdAt) {
        this(userId, username, email, role, null, createdAt);
    }

    public static UserResponseDto fromEntity(Users user) {
        if (user == null) {
            return null;
        }
        return new UserResponseDto(
            user.getUser_id(),
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            user.getProvider(),
            user.getCreated_at()
        );
    }
}

