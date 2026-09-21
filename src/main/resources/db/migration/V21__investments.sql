-- Modulo Investimentos: lista global por usuario (nao por aba), separado de Gastos/Devedores/Bets.
CREATE TABLE investments (
    id                    UUID PRIMARY KEY,
    user_id               UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name                  VARCHAR(255) NOT NULL,
    amount                NUMERIC(14, 2) NOT NULL,
    monthly_rate_percent  NUMERIC(7, 4) NOT NULL,
    created_at            TIMESTAMP NOT NULL
);
CREATE INDEX idx_investments_user_id ON investments (user_id);
