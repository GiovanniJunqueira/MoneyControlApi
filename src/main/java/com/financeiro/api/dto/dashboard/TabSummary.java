package com.financeiro.api.dto.dashboard;

import java.math.BigDecimal;
import java.util.UUID;

public record TabSummary(UUID id, String name, String color, BigDecimal totalGastoPeriodo, BigDecimal totalPendente) {
}
