package com.financeiro.api.dto.dashboard;

import java.util.List;

public record DevedoresGeralResponse(
        String periodKey,
        boolean total,
        List<DevedorGeralResponse> devedores
) {
}
