package com.example.delivery.user.dto.response;

import lombok.Getter;

@Getter
public class TokenResponse {

    private final String accessToken;
    private final String tokenType;

    public TokenResponse(String accessToken, String tokenType) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
    }
}
