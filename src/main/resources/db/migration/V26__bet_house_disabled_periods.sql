-- Desabilitar/habilitar casa por período (substitui bet_houses.archived_from, que só sabia
-- "escondida de X em diante pra sempre" - restaurar fazia a casa reaparecer retroativamente também
-- nos dias em que ela ficou desabilitada). to_date é exclusivo: a casa volta a aparecer nesse dia.
-- to_date NULL = ainda desabilitada.
CREATE TABLE bet_house_disabled_periods (
    id        UUID PRIMARY KEY,
    house_id  UUID NOT NULL REFERENCES bet_houses (id) ON DELETE CASCADE,
    from_date DATE NOT NULL,
    to_date   DATE
);
CREATE INDEX idx_bet_house_disabled_periods_house_id ON bet_house_disabled_periods (house_id);

INSERT INTO bet_house_disabled_periods (id, house_id, from_date)
SELECT gen_random_uuid(), id, archived_from FROM bet_houses WHERE archived_from IS NOT NULL;

ALTER TABLE bet_houses DROP COLUMN archived_from;
