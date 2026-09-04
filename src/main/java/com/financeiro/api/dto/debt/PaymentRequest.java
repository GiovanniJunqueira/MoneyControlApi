package com.financeiro.api.dto.debt;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentRequest(
        @NotNull(message = "Valor obrigatório.") @DecimalMin(value = "0.01", message = "O valor do pagamento precisa ser maior que zero.") BigDecimal amount,
        LocalDateTime date
) {
}
