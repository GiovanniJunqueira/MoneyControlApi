-- "Excluir" uma casa deixa de ser um DELETE de verdade - passa a ser um arquivamento a partir de
-- uma data, sem apagar nenhum bet_daily_balances/bet_month_starting_balances que ela já tinha.
ALTER TABLE bet_houses ADD COLUMN archived_from DATE;
