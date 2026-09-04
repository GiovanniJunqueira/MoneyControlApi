package com.financeiro.api.dto.dashboard;

import java.math.BigDecimal;

public record DevedoresResumo(BigDecimal totalEmprestado, BigDecimal totalRecebido, BigDecimal totalPendente, long quantidadePessoas) {
}
