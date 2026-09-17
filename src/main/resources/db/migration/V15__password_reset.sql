-- Token de reset de senha (fluxo "esqueci minha senha"), enviado por e-mail.
-- Um usuario tem no maximo um token ativo por vez (gerar um novo sobrescreve o anterior).
ALTER TABLE users ADD COLUMN reset_token VARCHAR(64);
ALTER TABLE users ADD COLUMN reset_token_expires_at TIMESTAMP;

-- Indice parcial: unico entre os tokens realmente ativos (NULL nao entra na unicidade).
CREATE UNIQUE INDEX idx_users_reset_token ON users (reset_token) WHERE reset_token IS NOT NULL;
