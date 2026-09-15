-- Sem isso, duas mudancas de unidade no mesmo dia empatam no ORDER BY date DESC e a ordem fica indefinida.
ALTER TABLE bet_unit_value_changes ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();
