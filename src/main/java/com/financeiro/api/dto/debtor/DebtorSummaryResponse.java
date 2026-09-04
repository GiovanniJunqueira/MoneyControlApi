package com.financeiro.api.dto.debtor;

import java.math.BigDecimal;
import java.util.UUID;

public record DebtorSummaryResponse(UUID id, String name, String notes, BigDecimal totalDevido, long quantidadeDividas) {
}
