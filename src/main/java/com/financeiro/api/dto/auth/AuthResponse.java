package com.financeiro.api.dto.auth;

public record AuthResponse(UserResponse user, String token) {
}
