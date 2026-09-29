package com.financeiro.api.dto.dashboard;

import java.math.BigDecimal;
import java.util.UUID;

/** percentual = % do gasto total do período que essa categoria representa (fatia do gráfico).
 * orcamento/percentualOrcamento = limite mensal opcional da categoria e quanto dele já foi usado -
 * conceito diferente do percentual acima, null quando a categoria não tem orçamento definido. */
public record CategoriaResumo(UUID categoryId, String nome, String cor, BigDecimal total, long quantidade, double percentual,
                               BigDecimal orcamento, Double percentualOrcamento) {
}
