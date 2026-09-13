package com.finledger.account.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
