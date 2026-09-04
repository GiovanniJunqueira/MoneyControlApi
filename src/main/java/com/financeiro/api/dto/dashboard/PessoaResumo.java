package com.financeiro.api.dto.dashboard;

import com.financeiro.api.dto.debt.DebtResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PessoaResumo(UUID debtorId, String nome, BigDecimal totalDevido, BigDecimal totalPago, List<DebtResponse> dividas) {
}
