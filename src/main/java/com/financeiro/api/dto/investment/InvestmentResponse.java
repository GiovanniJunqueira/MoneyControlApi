package com.financeiro.api.dto.investment;

import java.math.BigDecimal;
import java.util.UUID;

public record InvestmentResponse(UUID id, String name, BigDecimal amount, BigDecimal monthlyRatePercent) {
}
