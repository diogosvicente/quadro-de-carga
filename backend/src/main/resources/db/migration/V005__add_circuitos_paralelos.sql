-- Feature 6b (Rodrigo, 22/07/2026): condutores em paralelo por fase.
-- P = quantos cabos em paralelo passam por fase (não altera disjuntor/correntes/fatores).
-- Coluna com DEFAULT 1 cobre as linhas já existentes (seed V003) sem editar migrations aplicadas.
ALTER TABLE circuito
    ADD COLUMN circuitos_paralelos INT NOT NULL DEFAULT 1 CHECK (circuitos_paralelos >= 1);

ALTER TABLE quadro
    ADD COLUMN circuitos_paralelos INT NOT NULL DEFAULT 1 CHECK (circuitos_paralelos >= 1);
