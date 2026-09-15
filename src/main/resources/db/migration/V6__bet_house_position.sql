-- Ordem explicita das casas (antes era so por nome) - permite o usuario arrastar pra reordenar.
ALTER TABLE bet_houses ADD COLUMN position INT NOT NULL DEFAULT 0;

UPDATE bet_houses b
SET position = sub.rn
FROM (
    SELECT id, (ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at) - 1)::int AS rn
    FROM bet_houses
) sub
WHERE b.id = sub.id;
