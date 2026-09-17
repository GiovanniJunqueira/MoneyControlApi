-- Corrige o snapshot de saldo inicial (bet_month_starting_balances) de meses de backfill
-- (totalmente no passado, end_date preenchido) que foram criados usando o saldo ATUAL de cada
-- casa em vez do saldo que ela tinha ANTES daquele mês começar. Isso fazia uma casa criada/
-- alimentada só em um mês posterior (ex: redistribuição de "restante das casas" feita no mês
-- corrente) vazar seu saldo de HOJE pra trás, inflando a banca exibida de meses antigos em que
-- ela nem existia ainda. Não afeta profitLoss/profitLossUnits (sempre foram calculados a partir
-- do resultado dia a dia, nunca desse snapshot) - só corrige o número de "banca" exibido.
UPDATE bet_month_starting_balances smb
SET balance = COALESCE(
    (SELECT dbal.balance
     FROM bet_daily_balances dbal
     WHERE dbal.house_id = smb.house_id
       AND dbal.date < (SELECT bm.start_date FROM bet_months bm WHERE bm.id = smb.month_id)
     ORDER BY dbal.date DESC
     LIMIT 1),
    0
)
WHERE smb.month_id IN (SELECT id FROM bet_months WHERE end_date IS NOT NULL);
