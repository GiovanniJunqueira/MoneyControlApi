package com.financeiro.api.security;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public class CurrentUser {

    private CurrentUser() {
    }

    /** Retorna o ID do usuário autenticado, extraído do JWT pelo JwtAuthenticationFilter. */
    public static UUID id() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
