CREATE TABLE circuito (
    id                    BIGSERIAL PRIMARY KEY,
    quadro_id             BIGINT        NOT NULL REFERENCES quadro (id) ON DELETE CASCADE,
    numero                INT           NOT NULL CHECK (numero > 0),
    descricao             VARCHAR(120),
    tipo                  VARCHAR(20)   NOT NULL DEFAULT 'FORCA',
    tensao_v              INT           NOT NULL CHECK (tensao_v IN (127, 220, 380, 440, 660)),
    fases                 INT           NOT NULL DEFAULT 1 CHECK (fases BETWEEN 1 AND 3),
    potencia_w            NUMERIC(12, 2) NOT NULL CHECK (potencia_w > 0),
    fator_potencia        NUMERIC(4, 3) NOT NULL DEFAULT 0.92 CHECK (fator_potencia > 0 AND fator_potencia <= 1),
    comprimento_m         NUMERIC(8, 2) NOT NULL CHECK (comprimento_m > 0),
    circuitos_agrupados   INT           NOT NULL DEFAULT 1 CHECK (circuitos_agrupados >= 1),
    temperatura_c         INT           NOT NULL DEFAULT 30,
    metodo_instalacao     VARCHAR(4)    NOT NULL DEFAULT 'B1',
    isolante              VARCHAR(10)   NOT NULL DEFAULT 'PVC',
    forma_agrupamento_ref INT           NOT NULL DEFAULT 1,
    fator_demanda         NUMERIC(5, 3) NOT NULL DEFAULT 1 CHECK (fator_demanda > 0 AND fator_demanda <= 1),
    queda_admissivel_pct  NUMERIC(4, 2) NOT NULL DEFAULT 4 CHECK (queda_admissivel_pct > 0),
    linha_subterranea     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_circuito_quadro_numero ON circuito (quadro_id, numero);
CREATE INDEX ix_circuito_quadro ON circuito (quadro_id);
