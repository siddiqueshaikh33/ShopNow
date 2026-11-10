package com.example.demo.Dto;

import com.example.demo.Entity.Users;

public class TokenPairResponse {

    private final String accessToken;
    private final String refreshToken;
    private final Users user;

    public TokenPairResponse(String accessToken, String refreshToken, Users user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Users getUser() {
        return user;
    }
}

