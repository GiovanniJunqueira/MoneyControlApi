-- Preferencia do usuario pra habilitar o modulo separado de Bets (apostas).
ALTER TABLE users ADD COLUMN bets_enabled BOOLEAN NOT NULL DEFAULT false;
