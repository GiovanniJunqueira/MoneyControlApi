package com.financeiro.api.dto.debt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DebtResponse(UUID id, BigDecimal amount, String reason, LocalDate date, String status, BigDecimal paidAmount) {
}
