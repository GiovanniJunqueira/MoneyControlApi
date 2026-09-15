package com.financeiro.api.dto.dashboard;

import java.math.BigDecimal;
import java.util.UUID;

public record DevedorGeralResponse(
        UUID debtorId,
        String nome,
        BigDecimal totalDevido,
        long quantidadeDividas,
        UUID tabId,
        String tabName,
        String tabColor
) {
}
