-- Equipamentos do circuito (docs/CALCULOS.md §1.1b): lista opcional de quantidades × valores
-- unitários (potência OU corrente) que deriva a potência total persistida em circuito.potencia_w.
CREATE TABLE equipamento (
    id          BIGSERIAL PRIMARY KEY,
    circuito_id BIGINT         NOT NULL REFERENCES circuito (id) ON DELETE CASCADE,
    nome        VARCHAR(120),
    quantidade  INT            NOT NULL CHECK (quantidade >= 1),
    potencia_w  NUMERIC(12, 2) CHECK (potencia_w > 0),
    corrente_a  NUMERIC(10, 2) CHECK (corrente_a > 0),
    ordem       INT            NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    -- exatamente um dos dois valores unitários (modo potência OU modo corrente)
    CHECK ((potencia_w IS NOT NULL) <> (corrente_a IS NOT NULL))
);

CREATE INDEX ix_equipamento_circuito ON equipamento (circuito_id);
