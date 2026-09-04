package com.financeiro.api.dto.dashboard;

import java.math.BigDecimal;

public record GastosResumo(BigDecimal total, long quantidadeLancamentos, BigDecimal mediaPorLancamento) {
}
