-- Antes, a "banca final" de um mes fechado era so INFERIDA como a startingBanca do proximo mes -
-- o que quebra se esse proximo mes for excluido (feature nova de apagar mes). Agora fica gravada
-- no proprio mes, no momento em que ele fecha (em BetService.startMonth()).
ALTER TABLE bet_months ADD COLUMN ending_banca NUMERIC(12, 2);

-- Backfill best-effort pros meses ja fechados: usa a startingBanca do proximo mes (cronologicamente).
-- Se esse proximo mes ja tiver sido excluido antes dessa migration rodar, fica NULL (cai no fallback
-- antigo no codigo).
UPDATE bet_months m1
SET ending_banca = (
    SELECT m2.starting_banca
    FROM bet_months m2
    WHERE m2.user_id = m1.user_id
      AND (m2.start_date, m2.created_at) > (m1.start_date, m1.created_at)
    ORDER BY m2.start_date ASC, m2.created_at ASC
    LIMIT 1
)
WHERE m1.end_date IS NOT NULL;
