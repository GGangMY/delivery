package com.example.delivery.user.dto.request;

import com.example.delivery.user.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(@NotBlank @Size(min = 4, max = 20) String username,
                            @NotBlank @Size(min = 8) String password,
                            @NotNull UserRole role) {
}