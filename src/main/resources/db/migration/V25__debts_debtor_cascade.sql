-- Corrige FK sem cascade em debts.debtor_id (excluir um devedor com dívidas registradas quebrava
-- com violação de FK, mesma classe de bug já corrigida em V2 pra debt_payments.debt_id).
ALTER TABLE debts DROP CONSTRAINT debts_debtor_id_fkey;
ALTER TABLE debts ADD CONSTRAINT debts_debtor_id_fkey
    FOREIGN KEY (debtor_id) REFERENCES debtors (id) ON DELETE CASCADE;
