package com.example.delivery.user.dto.response;

import com.example.delivery.user.entity.UserRole;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserResponse {

    private final Long userId;
    private final String username;
    private final UserRole role;
    private final LocalDateTime createdAt;

    public UserResponse(Long userId, String username, UserRole role, LocalDateTime createdAt) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.createdAt = createdAt;
    }
}
