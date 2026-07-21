-- Quadro de demonstração com os dois circuitos usados no teste do app Glide (WhatsApp 21/07/2026),
-- para o sistema já abrir com dados reais do departamento.
INSERT INTO quadro (nome, local, sistema_tensao, fases_alimentador, fator_demanda, carga_reserva_va,
                    comprimento_m, metodo_instalacao, queda_admissivel_pct, temperatura_c)
VALUES ('QDC Exemplo', 'Departamento de Engenharia Elétrica — UERJ', 'V127_220', 3, 1, 0, 20, 'B1', 2, 30);

INSERT INTO circuito (quadro_id, numero, descricao, tipo, tensao_v, fases, potencia_w, fator_potencia,
                      comprimento_m, circuitos_agrupados, temperatura_c, metodo_instalacao, isolante,
                      forma_agrupamento_ref, fator_demanda, queda_admissivel_pct, linha_subterranea)
VALUES
    ((SELECT id FROM quadro WHERE nome = 'QDC Exemplo'), 1, 'Quadro de comando', 'FORCA', 220, 1,
     5900, 1, 25, 2, 40, 'B1', 'PVC', 1, 1, 4, FALSE),
    ((SELECT id FROM quadro WHERE nome = 'QDC Exemplo'), 2, 'Freezers', 'FORCA', 220, 1,
     1419, 0.92, 19, 1, 30, 'B1', 'PVC', 1, 1, 4, FALSE);
