-- Permite corrigir o saldo INICIAL de um dia (ex: depois de um deposito/saque manual na casa),
-- separando "resultado da aposta" de "movimentacao de dinheiro" (deposito/saque nao deve contar
-- como lucro/prejuizo). NULL = usa o carry-forward automatico (saldo final do dia anterior, ou o
-- snapshot do mes pro dia 1) - so fica preenchido quando o usuario explicitamente ajusta o inicio.
ALTER TABLE bet_daily_balances ADD COLUMN opening_balance NUMERIC(12, 2);
