-- Substituida por calculo dinamico (sempre recomputado a partir de bet_daily_balances) em BetService,
-- necessario pra permitir editar dias de qualquer mes (nao so o aberto) sem deixar esse valor obsoleto.
ALTER TABLE bet_months DROP COLUMN ending_banca;
