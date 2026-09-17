-- Reaplica o mesmo recalculo da V16 (banca de mês de backfill = saldo de cada casa ANTES do mês
-- começar, não o saldo atual de agora). Precisou rodar de novo porque, depois da V16, a casa
-- Superbet ganhou uma entrada explícita zerando ela em 2025-08-01 (ela parou de ser usada e o
-- usuário confirmou que o saldo foi sacado, só nunca registrado na planilha original) - os meses
-- de backfill posteriores a essa data ainda tinham o snapshot ANTIGO (de antes dessa entrada
-- existir) guardado em bet_month_starting_balances, que não se recalcula sozinho.
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
