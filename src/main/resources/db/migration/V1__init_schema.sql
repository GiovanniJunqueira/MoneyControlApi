-- Schema inicial, espelhando as entidades JPA existentes (substitui o antigo ddl-auto: update).

CREATE TABLE users (
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE categories (
    id      UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    name    VARCHAR(255) NOT NULL,
    color   VARCHAR(255),
    icon    VARCHAR(255),
    CONSTRAINT uk_categories_user_name UNIQUE (user_id, name)
);

CREATE TABLE debtors (
    id      UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    name    VARCHAR(255) NOT NULL,
    notes   VARCHAR(255)
);

CREATE TABLE expenses (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users (id),
    category_id UUID NOT NULL REFERENCES categories (id),
    amount      NUMERIC(12, 2) NOT NULL,
    description VARCHAR(255),
    date        DATE NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP
);

CREATE TABLE debts (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users (id),
    debtor_id   UUID NOT NULL REFERENCES debtors (id),
    amount      NUMERIC(12, 2) NOT NULL,
    reason      VARCHAR(255) NOT NULL,
    date        DATE NOT NULL,
    status      VARCHAR(255) NOT NULL,
    paid_amount NUMERIC(12, 2) NOT NULL,
    created_at  TIMESTAMP NOT NULL
);

CREATE TABLE debt_payments (
    id      UUID PRIMARY KEY,
    debt_id UUID NOT NULL REFERENCES debts (id),
    amount  NUMERIC(12, 2) NOT NULL,
    date    TIMESTAMP NOT NULL
);

CREATE TABLE module_settings (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users (id),
    module      VARCHAR(255) NOT NULL,
    closing_day INTEGER NOT NULL,
    CONSTRAINT uk_module_settings_user_module UNIQUE (user_id, module)
);

CREATE INDEX idx_categories_user_id ON categories (user_id);
CREATE INDEX idx_debtors_user_id ON debtors (user_id);
CREATE INDEX idx_expenses_user_id ON expenses (user_id);
CREATE INDEX idx_expenses_category_id ON expenses (category_id);
CREATE INDEX idx_debts_user_id ON debts (user_id);
CREATE INDEX idx_debts_debtor_id ON debts (debtor_id);
CREATE INDEX idx_debt_payments_debt_id ON debt_payments (debt_id);
CREATE INDEX idx_module_settings_user_id ON module_settings (user_id);
