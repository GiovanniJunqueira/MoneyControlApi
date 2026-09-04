package com.financeiro.api.util;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Lógica do "período fiscal" customizável.
 * <p>
 * Em vez de usar o mês de calendário, cada módulo tem um {@code closingDay}
 * (dia de fechamento, ex: 25). O período vai do dia (closingDay + 1) de um
 * mês até o closingDay do mês seguinte.
 * <p>
 * Exemplo com closingDay = 25:
 * - Um lançamento em 10/08 pertence ao período que fecha em 25/08 (início 26/07, fim 25/08).
 * - Um lançamento em 26/08 já pertence ao próximo período (início 26/08, fim 25/09).
 */
public class FiscalPeriodCalculator {

    private FiscalPeriodCalculator() {
    }

    private static int clampDay(int year, int month, int day) {
        int lastDay = YearMonth.of(year, month).lengthOfMonth();
        return Math.min(day, lastDay);
    }

    public static FiscalPeriod getFiscalPeriod(LocalDate referenceDate, int closingDay) {
        int year = referenceDate.getYear();
        int month = referenceDate.getMonthValue();
        int day = referenceDate.getDayOfMonth();

        int thisMonthClosing = clampDay(year, month, closingDay);

        int periodEndYear = year;
        int periodEndMonth = month;

        if (day > thisMonthClosing) {
            periodEndMonth += 1;
            if (periodEndMonth > 12) {
                periodEndMonth = 1;
                periodEndYear += 1;
            }
        }

        int periodEndDay = clampDay(periodEndYear, periodEndMonth, closingDay);
        LocalDate end = LocalDate.of(periodEndYear, periodEndMonth, periodEndDay);

        int startMonth = periodEndMonth - 1;
        int startYear = periodEndYear;
        if (startMonth < 1) {
            startMonth = 12;
            startYear -= 1;
        }
        int startClosingDay = clampDay(startYear, startMonth, closingDay);
        LocalDate start = LocalDate.of(startYear, startMonth, startClosingDay).plusDays(1);

        String key = String.format("%d-%02d", periodEndYear, periodEndMonth);

        return new FiscalPeriod(key, start, end, thisMonthClosing);
    }

    public static FiscalPeriod getCurrentFiscalPeriod(int closingDay) {
        return getFiscalPeriod(LocalDate.now(), closingDay);
    }
}
