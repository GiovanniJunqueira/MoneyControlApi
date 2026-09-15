-- Modulo Bets (controle de banca de apostas), completamente separado das abas/Financeiro.
-- So existe pra usuarios com bets_enabled = true, mas as tabelas existem pra todo mundo.

CREATE TABLE bet_houses (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    color      VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_bet_houses_user_id ON bet_houses (user_id);

CREATE TABLE bet_months (
    id                 UUID PRIMARY KEY,
    user_id            UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    start_date         DATE NOT NULL,
    end_date           DATE,
    initial_unit_value NUMERIC(12, 2) NOT NULL,
    starting_banca     NUMERIC(12, 2) NOT NULL
);
CREATE INDEX idx_bet_months_user_id ON bet_months (user_id);
-- So pode existir um mes aberto (end_date NULL) por usuario.
CREATE UNIQUE INDEX uk_bet_months_user_open ON bet_months (user_id) WHERE end_date IS NULL;

CREATE TABLE bet_unit_value_changes (
    id       UUID PRIMARY KEY,
    month_id UUID NOT NULL REFERENCES bet_months (id) ON DELETE CASCADE,
    date     DATE NOT NULL,
    value    NUMERIC(12, 2) NOT NULL
);
CREATE INDEX idx_bet_unit_value_changes_month_id ON bet_unit_value_changes (month_id);

CREATE TABLE bet_daily_balances (
    id       UUID PRIMARY KEY,
    house_id UUID NOT NULL REFERENCES bet_houses (id) ON DELETE CASCADE,
    month_id UUID NOT NULL REFERENCES bet_months (id) ON DELETE CASCADE,
    date     DATE NOT NULL,
    balance  NUMERIC(12, 2) NOT NULL,
    CONSTRAINT uk_bet_daily_balances_house_date UNIQUE (house_id, date)
);
CREATE INDEX idx_bet_daily_balances_house_id ON bet_daily_balances (house_id);
CREATE INDEX idx_bet_daily_balances_month_id ON bet_daily_balances (month_id);
