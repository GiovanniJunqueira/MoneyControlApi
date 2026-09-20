-- Reverte a feature de Amigos (V19) - desistiu de mante-la. Nao apaga a V19 (ja aplicada em
-- producao, Flyway quebraria a validacao se o arquivo sumisse), so desfaz o schema que ela criou.
DROP TABLE IF EXISTS bet_friend_requests;
DROP TABLE IF EXISTS bet_friendships;
DROP INDEX IF EXISTS idx_users_friend_code;
ALTER TABLE users DROP COLUMN IF EXISTS friend_code;
