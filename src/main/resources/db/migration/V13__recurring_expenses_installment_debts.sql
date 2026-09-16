-- Gastos recorrentes: varias linhas reais (uma por mes) ligadas por um group id. Sem tabela de
-- template - "indefinido" so significa que um horizonte grande de meses foi materializado de uma vez.
ALTER TABLE expenses ADD COLUMN recurring_group_id UUID;
CREATE INDEX idx_expenses_recurring_group_id ON expenses (recurring_group_id);

-- Dividas parceladas: mesma ideia - N dividas reais (uma por mes), cada uma com seu proprio
-- status/pagamento, ligadas por installment_group_id.
ALTER TABLE debts ADD COLUMN installment_group_id UUID;
ALTER TABLE debts ADD COLUMN installment_number INT;
ALTER TABLE debts ADD COLUMN installment_total INT;
CREATE INDEX idx_debts_installment_group_id ON debts (installment_group_id);
