package com.financeiro.api.dto.debtor;

import jakarta.validation.constraints.NotBlank;

public record DebtorRequest(@NotBlank(message = "Nome obrigatório.") String name, String notes) {
}
