package com.financeiro.api.dto.debt;

import java.util.List;
import java.util.UUID;

public record DebtorDetailResponse(UUID id, String name, String notes, List<DebtResponse> debts) {
}
