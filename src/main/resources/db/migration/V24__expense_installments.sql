-- Gastos parcelados: mesma ideia das dividas parceladas (V13) - N gastos reais (um por mes), cada
-- um com seu proprio valor (a divisao do total), ligados por installment_group_id. Separado de
-- recurring_group_id (ja existente) - recorrencia repete o MESMO valor todo mes, parcelamento
-- DIVIDE um valor total entre os meses.
ALTER TABLE expenses ADD COLUMN installment_group_id UUID;
ALTER TABLE expenses ADD COLUMN installment_number INT;
ALTER TABLE expenses ADD COLUMN installment_total INT;
CREATE INDEX idx_expenses_installment_group_id ON expenses (installment_group_id);
