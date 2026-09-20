-- Amigos do modulo Bets: cada usuario tem um codigo permanente (gerado sob demanda, nao aqui);
-- convite pendente vira amizade mutua quando o destinatario aceita (ou quando ele passa o codigo
-- de quem ja o convidou, o que fecha a amizade na hora).
ALTER TABLE users ADD COLUMN friend_code VARCHAR(10);
CREATE UNIQUE INDEX idx_users_friend_code ON users (friend_code) WHERE friend_code IS NOT NULL;

CREATE TABLE bet_friend_requests (
    id          UUID PRIMARY KEY,
    sender_id   UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL,
    CONSTRAINT uk_bet_friend_requests UNIQUE (sender_id, receiver_id)
);
CREATE INDEX idx_bet_friend_requests_receiver_id ON bet_friend_requests (receiver_id);
CREATE INDEX idx_bet_friend_requests_sender_id ON bet_friend_requests (sender_id);

-- user_one_id e sempre o menor UUID do par (ver BetFriendService.sortPair) - garante que a mesma
-- dupla nunca gera duas linhas diferentes dependendo de quem virou amigo de quem primeiro.
CREATE TABLE bet_friendships (
    id          UUID PRIMARY KEY,
    user_one_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    user_two_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL,
    CONSTRAINT uk_bet_friendships UNIQUE (user_one_id, user_two_id)
);
CREATE INDEX idx_bet_friendships_user_one_id ON bet_friendships (user_one_id);
CREATE INDEX idx_bet_friendships_user_two_id ON bet_friendships (user_two_id);
