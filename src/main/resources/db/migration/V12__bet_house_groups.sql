-- Agrupamento de casas: permite juntar varias casas (ex: sub-contas de uma casa mae) num grupo,
-- pra ver o resultado combinado do grupo tanto no dia quanto no mes, sem perder o resultado individual
-- de cada casa.

CREATE TABLE bet_house_groups (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_bet_house_groups_user_id ON bet_house_groups (user_id);

ALTER TABLE bet_houses ADD COLUMN group_id UUID REFERENCES bet_house_groups (id) ON DELETE SET NULL;
CREATE INDEX idx_bet_houses_group_id ON bet_houses (group_id);
