package com.financeiro.api.dto.dashboard;

import java.util.List;

public record VisaoGeralResponse(
        String periodKey,
        List<TabSummary> abas,
        GastosResumo resumoGastos,
        List<CategoriaResumo> porCategoria,
        DevedoresResumo resumoDevedores,
        List<PessoaResumo> porPessoa
) {
}
