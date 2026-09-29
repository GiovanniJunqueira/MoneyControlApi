package com.financeiro.api.dto.category;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryResponse(UUID id, String name, String color, String icon, BigDecimal monthlyBudget) {
}
