package com.dbtraining.reconx.dto;

public record LoginResponse(String token, String tokenType, long expiresInSeconds, String role) {
}
