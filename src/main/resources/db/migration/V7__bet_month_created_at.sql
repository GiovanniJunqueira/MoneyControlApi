-- Sem isso, dois meses iniciados no mesmo dia (ex: corrigindo um teste, ou "iniciar novo mes" duas vezes
-- no mesmo dia) empatam no ORDER BY start_date DESC e a ordem cronologica fica indefinida.
ALTER TABLE bet_months ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();
