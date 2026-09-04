package com.financeiro.api.util;

import java.time.LocalDate;

/**
 * Representa um período fiscal (mês customizado com base no dia de fechamento).
 */
public record FiscalPeriod(String key, LocalDate start, LocalDate end, int closingDay) {
}
