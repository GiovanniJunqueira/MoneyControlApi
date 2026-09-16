-- Competicao mensal do modulo Bets: uma pessoa cria (vira o primeiro membro automaticamente), gera
-- um codigo, outras pessoas entram com o codigo e veem o ranking do resultado do mes escolhido.
CREATE TABLE bet_competitions (
    id         UUID PRIMARY KEY,
    creator_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    code       VARCHAR(12) NOT NULL UNIQUE,
    year       INT NOT NULL,
    month      INT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_bet_competitions_creator_id ON bet_competitions (creator_id);

CREATE TABLE bet_competition_members (
    id             UUID PRIMARY KEY,
    competition_id UUID NOT NULL REFERENCES bet_competitions (id) ON DELETE CASCADE,
    user_id        UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    joined_at      TIMESTAMP NOT NULL,
    CONSTRAINT uk_bet_competition_members UNIQUE (competition_id, user_id)
);
CREATE INDEX idx_bet_competition_members_competition_id ON bet_competition_members (competition_id);
CREATE INDEX idx_bet_competition_members_user_id ON bet_competition_members (user_id);
