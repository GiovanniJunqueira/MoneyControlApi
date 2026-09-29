-- Orcamento mensal por categoria (null = sem orcamento definido).
ALTER TABLE categories ADD COLUMN monthly_budget NUMERIC(12, 2);

-- Divisao de gasto: liga uma divida (Devedores) ao gasto que a originou. Excluir o gasto tambem
-- exclui a divida gerada por ele - ela nao tem sentido sem a origem.
ALTER TABLE debts ADD COLUMN source_expense_id UUID REFERENCES expenses (id) ON DELETE CASCADE;
CREATE INDEX idx_debts_source_expense_id ON debts (source_expense_id);
