package com.example.delivery.menu.dto.response;

import java.time.LocalDateTime;

public record MenuResponse(Long menuId, String name, Long price, String description, String owner,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
}