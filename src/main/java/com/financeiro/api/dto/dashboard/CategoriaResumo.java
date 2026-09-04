package com.financeiro.api.dto.dashboard;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoriaResumo(UUID categoryId, String nome, String cor, BigDecimal total, long quantidade, double percentual) {
}
