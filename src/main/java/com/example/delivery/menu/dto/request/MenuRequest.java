package com.example.delivery.menu.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MenuRequest(@NotBlank String name, @NotNull @Min(1) Long price, String description) {
}