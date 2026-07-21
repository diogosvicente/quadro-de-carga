-- A regra de negócio compara nomes ignorando caixa (existsByNomeIgnoreCase); o índice único
-- precisa proteger a mesma regra, senão dois POSTs concorrentes gravam "Quadro X" e "quadro x".
DROP INDEX IF EXISTS ux_quadro_nome;
CREATE UNIQUE INDEX ux_quadro_nome ON quadro (lower(nome));
