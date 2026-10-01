package com.financeiro.api.util;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Lógica do "período fiscal" customizável.
 * <p>
 * Em vez de usar o mês de calendário, cada módulo tem um {@code closingDay} (dia de fechamento,
 * ex: 25). <b>O dia do fechamento é o PRIMEIRO dia do período novo</b>, não o último dia do
 * período antigo - pedido explícito do usuário: "dia 1 fecha, dia 1 já conta pro próximo, não pra
 * fatura anterior" (ele lançou uma dívida exatamente no dia do fechamento e ela precisava cair no
 * ciclo que estava começando, não no que estava terminando). O período vai do dia
 * {@code closingDay} de um mês até o dia ({@code closingDay} − 1) do mês seguinte.
 * <p>
 * Exemplo com closingDay = 25:
 * - Um lançamento em 10/08 pertence ao período [25/07, 24/08] (fecha de novo em 25/08, quando o
 *   próximo período já começa).
 * - Um lançamento em 25/08 já pertence ao próximo período [25/08, 24/09].
 * <p>
 * Caso especial importante: {@code closingDay = 1} (o padrão de toda aba nova que ninguém
 * configurou) faz o período virar EXATAMENTE o mês de calendário ([1, último dia do mês]) - é o
 * comportamento mais intuitivo possível pra quem nunca mexeu nessa configuração.
 * <p>
 * <b>Qual "mês" (key) um período leva o nome</b>: nomeado pelo mês onde cai a MAIORIA dos seus
 * dias - um limiar fixo e simples (closingDay < 15 → nomeado pelo mês em que COMEÇA; closingDay
 * >= 15 → nomeado pelo mês em que TERMINA) resolve isso sem precisar contar dias exatos (que
 * variam um pouco, meses têm 28-31 dias). Pra closingDay=1 isso não importa (o período já é
 * exatamente um mês de calendário, só tem um mês possível pra nomear). Como nenhuma "key" de
 * período é persistida em lugar nenhum (só a {@code date} de cada gasto/dívida, recalculado
 * sempre on-the-fly) - mudar esse critério não tem nenhum risco de migração ou dado desatualizado,
 * só muda o que aparece na tela ao navegar.
 */
public class FiscalPeriodCalculator {

    private static final int LABEL_BY_START_MONTH_THRESHOLD = 15;

    private FiscalPeriodCalculator() {
    }

    private static int clampDay(int year, int month, int day) {
        int lastDay = YearMonth.of(year, month).lengthOfMonth();
        return Math.min(day, lastDay);
    }

    private static int[] addMonths(int year, int month, int delta) {
        int total = (year * 12 + (month - 1)) + delta;
        int newYear = Math.floorDiv(total, 12);
        int newMonth = Math.floorMod(total, 12) + 1;
        return new int[]{newYear, newMonth};
    }

    public static FiscalPeriod getFiscalPeriod(LocalDate referenceDate, int closingDay) {
        int year = referenceDate.getYear();
        int month = referenceDate.getMonthValue();
        int day = referenceDate.getDayOfMonth();

        int thisMonthClosing = clampDay(year, month, closingDay);

        // dia >= fechamento deste mês → o período COMEÇA neste mês (o fechamento já é o primeiro
        // dia do novo ciclo). Senão, o período começou no mês anterior.
        int startYear = year;
        int startMonth = month;
        if (day < thisMonthClosing) {
            int[] prev = addMonths(year, month, -1);
            startYear = prev[0];
            startMonth = prev[1];
        }

        int startDay = clampDay(startYear, startMonth, closingDay);
        LocalDate start = LocalDate.of(startYear, startMonth, startDay);

        int[] next = addMonths(startYear, startMonth, 1);
        int endYear = next[0];
        int endMonth = next[1];
        int endClosingDay = clampDay(endYear, endMonth, closingDay);
        LocalDate end = LocalDate.of(endYear, endMonth, endClosingDay).minusDays(1);

        // ver javadoc da classe - nomeia pelo mês onde cai a maioria dos dias do período.
        String key = closingDay < LABEL_BY_START_MONTH_THRESHOLD
                ? String.format("%d-%02d", startYear, startMonth)
                : String.format("%d-%02d", endYear, endMonth);

        return new FiscalPeriod(key, start, end, thisMonthClosing);
    }

    public static FiscalPeriod getCurrentFiscalPeriod(int closingDay) {
        return getFiscalPeriod(LocalDate.now(), closingDay);
    }

    /**
     * Resolve o período cuja "key" (formato "YYYY-MM") é a informada - usado quando a pessoa navega
     * pra um mês específico (setas, seletor de mês). Tem que usar o MESMO critério de rotulagem de
     * getFiscalPeriod() pra ida e volta serem consistentes: se o período é nomeado pelo mês em que
     * começa (closingDay baixo), uma referência dentro do próprio mês pedido (no dia do fechamento)
     * já cai certinho; se é nomeado pelo mês em que termina (closingDay alto), a referência precisa
     * ser um dia do mês ANTERIOR (onde o período realmente começa) - o último dia do mês anterior
     * sempre serve, já que closingDay nunca passa de 28.
     */
    public static FiscalPeriod getFiscalPeriodForKey(int year, int month, int closingDay) {
        LocalDate reference;
        if (closingDay < LABEL_BY_START_MONTH_THRESHOLD) {
            reference = LocalDate.of(year, month, clampDay(year, month, closingDay));
        } else {
            reference = LocalDate.of(year, month, 1).minusDays(1);
        }
        return getFiscalPeriod(reference, closingDay);
    }
}
