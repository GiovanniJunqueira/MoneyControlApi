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
 * <p>
 * <b>Qual "mês" (key) um período leva o nome</b> - bug real relatado pelo usuário: com um
 * {@code closingDay} baixo (ex: 4, fechamento "dia 4 ao dia 5"), nomear o período sempre pelo mês
 * em que ele TERMINA (como no exemplo acima, pensado pra closingDay alto tipo 25) produz um
 * resultado contra-intuitivo - o período [05/set, 04/out] (29 dias em setembro, só 1 em outubro)
 * ficaria rotulado "Outubro". Uma dívida lançada em 24/set não aparecia ao navegar pra "Setembro",
 * porque "Setembro" (pelo critério antigo) significava o período [05/ago, 04/set]. Isso também já
 * afetava TODO mundo com o closingDay padrão (1, usado por toda aba nova que ninguém configurou):
 * o período corrente, a maior parte dos dias do mês, já saía rotulado com o mês SEGUINTE.
 * <p>
 * Correção: o período leva o nome do mês em que a MAIORIA dos seus dias cai. Como isso teria que
 * ser recalculado dia a dia (meses têm 28-31 dias, o "meio do período" varia um pouco), usamos um
 * limiar fixo e simples em vez de contar dias exatos - resultado idêntico pra qualquer closingDay
 * fora da faixa 13-17, e a decisão certa pros dois casos reais que já apareceram (25 → termina;
 * 1 e 4 → começa): {@code closingDay < 15} → nomeado pelo mês em que COMEÇA; {@code closingDay >=
 * 15} → nomeado pelo mês em que TERMINA (comportamento original, preservado pro exemplo acima).
 * Como nenhuma "key" de período é persistida em lugar nenhum (só a {@code date} de cada
 * gasto/dívida, recalculado sempre on-the-fly) - mudar esse critério não tem nenhum risco de
 * migração ou dado desatualizado, só muda o que aparece na tela ao navegar.
 */
public class FiscalPeriodCalculator {

    private static final int LABEL_BY_START_MONTH_THRESHOLD = 15;

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

        // ver javadoc da classe - nomeia pelo mês onde cai a maioria dos dias do período.
        String key = closingDay < LABEL_BY_START_MONTH_THRESHOLD
                ? String.format("%d-%02d", startYear, startMonth)
                : String.format("%d-%02d", periodEndYear, periodEndMonth);

        return new FiscalPeriod(key, start, end, thisMonthClosing);
    }

    public static FiscalPeriod getCurrentFiscalPeriod(int closingDay) {
        return getFiscalPeriod(LocalDate.now(), closingDay);
    }

    /**
     * Resolve o período cuja "key" (formato "YYYY-MM") é a informada - usado quando a pessoa navega
     * pra um mês específico (setas, seletor de mês). Tem que escolher o MESMO critério de rotulagem
     * usado em getFiscalPeriod() pra ida e volta serem consistentes: se o período é nomeado pelo mês
     * em que começa (closingDay baixo), a data de referência tem que cair logo no INÍCIO do período
     * desejado (closingDay + 1); se é nomeado pelo mês em que termina (closingDay alto, comportamento
     * original), a referência é o próprio closingDay, que já cai exatamente no fim do período.
     */
    public static FiscalPeriod getFiscalPeriodForKey(int year, int month, int closingDay) {
        int referenceDay = closingDay < LABEL_BY_START_MONTH_THRESHOLD ? closingDay + 1 : closingDay;
        LocalDate reference = LocalDate.of(year, month, clampDay(year, month, referenceDay));
        return getFiscalPeriod(reference, closingDay);
    }
}
