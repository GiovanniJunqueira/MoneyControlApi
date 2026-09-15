-- Abas: cada usuario pode ter multiplas "copias" do sistema (ex: separar por banco/conta).
-- Categoria, gasto, devedor, divida e config de periodo passam a pertencer a uma aba.

CREATE TABLE tabs (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    color      VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_tabs_user_name UNIQUE (user_id, name)
);
CREATE INDEX idx_tabs_user_id ON tabs (user_id);

-- Aba padrao "Geral" para cada usuario existente, para onde os dados atuais sao migrados.
INSERT INTO tabs (id, user_id, name, color, created_at)
SELECT gen_random_uuid(), id, 'Geral', '#007AFF', now() FROM users;

-- categories
ALTER TABLE categories ADD COLUMN tab_id UUID REFERENCES tabs (id) ON DELETE CASCADE;
UPDATE categories c SET tab_id = t.id FROM tabs t WHERE t.user_id = c.user_id;
ALTER TABLE categories ALTER COLUMN tab_id SET NOT NULL;
ALTER TABLE categories DROP CONSTRAINT uk_categories_user_name;
ALTER TABLE categories ADD CONSTRAINT uk_categories_tab_name UNIQUE (tab_id, name);
CREATE INDEX idx_categories_tab_id ON categories (tab_id);

-- expenses
ALTER TABLE expenses ADD COLUMN tab_id UUID REFERENCES tabs (id) ON DELETE CASCADE;
UPDATE expenses e SET tab_id = t.id FROM tabs t WHERE t.user_id = e.user_id;
ALTER TABLE expenses ALTER COLUMN tab_id SET NOT NULL;
CREATE INDEX idx_expenses_tab_id ON expenses (tab_id);

-- debtors
ALTER TABLE debtors ADD COLUMN tab_id UUID REFERENCES tabs (id) ON DELETE CASCADE;
UPDATE debtors d SET tab_id = t.id FROM tabs t WHERE t.user_id = d.user_id;
ALTER TABLE debtors ALTER COLUMN tab_id SET NOT NULL;
CREATE INDEX idx_debtors_tab_id ON debtors (tab_id);

-- debts
ALTER TABLE debts ADD COLUMN tab_id UUID REFERENCES tabs (id) ON DELETE CASCADE;
UPDATE debts d SET tab_id = t.id FROM tabs t WHERE t.user_id = d.user_id;
ALTER TABLE debts ALTER COLUMN tab_id SET NOT NULL;
CREATE INDEX idx_debts_tab_id ON debts (tab_id);

-- module_settings
ALTER TABLE module_settings ADD COLUMN tab_id UUID REFERENCES tabs (id) ON DELETE CASCADE;
UPDATE module_settings m SET tab_id = t.id FROM tabs t WHERE t.user_id = m.user_id;
ALTER TABLE module_settings ALTER COLUMN tab_id SET NOT NULL;
ALTER TABLE module_settings DROP CONSTRAINT uk_module_settings_user_module;
ALTER TABLE module_settings ADD CONSTRAINT uk_module_settings_tab_module UNIQUE (tab_id, module);
CREATE INDEX idx_module_settings_tab_id ON module_settings (tab_id);

-- Corrige FK sem cascade em debt_payments (excluir uma divida com pagamentos quebrava com
-- violacao de FK, bug pre-existente descoberto ao revisar essa area).
ALTER TABLE debt_payments DROP CONSTRAINT debt_payments_debt_id_fkey;
ALTER TABLE debt_payments ADD CONSTRAINT debt_payments_debt_id_fkey
    FOREIGN KEY (debt_id) REFERENCES debts (id) ON DELETE CASCADE;
