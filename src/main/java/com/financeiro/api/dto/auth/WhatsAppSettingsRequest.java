package com.financeiro.api.dto.auth;

/** phone em branco/null desliga o bot pra essa conta. */
public record WhatsAppSettingsRequest(String phone) {
}
