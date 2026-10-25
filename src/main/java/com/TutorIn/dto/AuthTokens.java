package com.TutorIn.dto;

public record AuthTokens(
        String accessToken,
        String refreshToken
){}
