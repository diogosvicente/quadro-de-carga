CREATE TABLE quadro (
    id                   BIGSERIAL PRIMARY KEY,
    nome                 VARCHAR(120)  NOT NULL,
    local                VARCHAR(200),
    sistema_tensao       VARCHAR(20)   NOT NULL DEFAULT 'V127_220',
    fases_alimentador    INT           NOT NULL DEFAULT 3 CHECK (fases_alimentador BETWEEN 1 AND 3),
    fator_demanda        NUMERIC(5, 3) NOT NULL DEFAULT 1 CHECK (fator_demanda > 0 AND fator_demanda <= 1),
    carga_reserva_va     NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (carga_reserva_va >= 0),
    comprimento_m        NUMERIC(8, 2) NOT NULL DEFAULT 10 CHECK (comprimento_m > 0),
    metodo_instalacao    VARCHAR(4)    NOT NULL DEFAULT 'B1',
    queda_admissivel_pct NUMERIC(4, 2) NOT NULL DEFAULT 2 CHECK (queda_admissivel_pct > 0),
    temperatura_c        INT           NOT NULL DEFAULT 30,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_quadro_nome ON quadro (nome);
