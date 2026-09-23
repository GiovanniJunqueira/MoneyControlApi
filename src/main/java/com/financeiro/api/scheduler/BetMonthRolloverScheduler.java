package com.financeiro.api.scheduler;

import com.financeiro.api.service.BetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Roda 1x/dia (00:10 horário de Brasília - já configurado via TimeZone.setDefault, ver
 * FinanceiroApiApplication) e fecha/abre automaticamente os meses do Bets que já viraram o mês
 * de calendário. Pedido explícito do usuário. */
@Slf4j
@Component
public class BetMonthRolloverScheduler {

    private final BetService betService;

    public BetMonthRolloverScheduler(BetService betService) {
        this.betService = betService;
    }

    @Scheduled(cron = "0 10 0 * * *", zone = "America/Sao_Paulo")
    public void run() {
        for (var monthId : betService.findExpiredOpenMonthIds()) {
            try {
                betService.rolloverMonth(monthId);
            } catch (Exception e) {
                // um mês com problema não pode travar a virada de todo mundo - loga e segue.
                log.error("Falha na virada automática do mês {}", monthId, e);
            }
        }
    }
}
