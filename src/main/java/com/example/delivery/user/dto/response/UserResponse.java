package com.example.delivery.user.dto.response;

import com.example.delivery.user.entity.UserRole;

import java.time.LocalDateTime;

public record UserResponse(Long userId, String username, UserRole role, LocalDateTime createdAt) {
}