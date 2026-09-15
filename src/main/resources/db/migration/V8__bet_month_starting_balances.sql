-- Snapshot do saldo de CADA casa no instante em que um mes comeca (a "startingBanca" do mes ja e
-- o total somado - isso aqui e a quebra por casa). Sem isso, quando dois meses comecam no mesmo dia
-- (ex: corrigir um erro de digitacao logo depois de iniciar), a query "saldo antes da data" nao
-- consegue distinguir "antes deste mes comecar" de "mais cedo hoje, no mes anterior", e o dia 1
-- do mes novo calcula um resultado por casa errado.
CREATE TABLE bet_month_starting_balances (
    id       UUID PRIMARY KEY,
    month_id UUID NOT NULL REFERENCES bet_months (id) ON DELETE CASCADE,
    house_id UUID NOT NULL REFERENCES bet_houses (id) ON DELETE CASCADE,
    balance  NUMERIC(12, 2) NOT NULL,
    CONSTRAINT uk_bet_month_starting_balances UNIQUE (month_id, house_id)
);
CREATE INDEX idx_bet_month_starting_balances_month_id ON bet_month_starting_balances (month_id);

-- Backfill best-effort pros meses ja existentes, com a mesma logica (por data) que estava sendo usada antes.
INSERT INTO bet_month_starting_balances (id, month_id, house_id, balance)
SELECT (md5(random()::text || clock_timestamp()::text))::uuid, m.id, h.id,
       COALESCE((
           SELECT b.balance FROM bet_daily_balances b
           WHERE b.house_id = h.id AND b.date < m.start_date
           ORDER BY b.date DESC LIMIT 1
       ), 0)
FROM bet_months m
JOIN bet_houses h ON h.user_id = m.user_id;
