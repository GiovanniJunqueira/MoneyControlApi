package com.financeiro.api.dto.bets;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Recorte do resultado mensal de um amigo, exposto por casa - deliberadamente sem o detalhe
 * dia-a-dia (só "resultado mensal por casa" foi pedido, mesmo espírito de escopo do ranking de
 * competição). */
public record BetFriendMonthDetailResponse(UUID monthId, LocalDate startDate, LocalDate endDate, boolean open,
                                            BigDecimal profitLoss, BigDecimal profitLossUnits,
                                            List<BetHouseMonthSummaryResponse> houseSummaries,
                                            List<BetHouseGroupMonthSummaryResponse> groupSummaries) {
}
