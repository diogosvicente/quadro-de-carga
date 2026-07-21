# Especificação de Cálculo — Dimensionamento de Circuitos (NBR 5410)

> Fonte da verdade do motor de cálculo do sistema. Consolidada a partir de:
> 1. Planilha `docs/iniciais/QUADRO DE CARGA_teste_queda de tensão_rev12.xlsx` (fórmulas);
> 2. Requisitos do Rodrigo (WhatsApp, 21/07/2026) e telas do app Glide (ilustrativas);
> 3. Tabelas da NBR 5410 extraídas da planilha para `docs/dados-normativos/*.json`.
>
> Onde a planilha contém erros evidentes (referências deslocadas nas linhas ≥ 6, `CONTARVALORES` inexistente,
> INDEX/MATCH da seção por sobrecorrente apontando para bloco auxiliar), esta especificação implementa a
> **intenção** da linha de referência (linha 5) e da norma. Divergências estão anotadas na §7.

---

## 1. Entradas

### 1.1 Circuito (cadastro pela equipe de campo)

| Campo | Tipo | Obrigatório | Padrão | Observações |
|---|---|---|---|---|
| `numero` | int > 0 | sim | — | único dentro do quadro |
| `descricao` | texto | não | — | ex.: "Iluminação Sala" |
| `tipo` | enum `ILUMINACAO` \| `FORCA` \| `SINALIZACAO` | sim | `FORCA` | tomadas contam como força (Tab. 47, nota 2) |
| `tensaoV` | enum 127, 220, 380, 440, 660 | sim | — | tensão de operação do circuito: F-N se monofásico, F-F se bi/trifásico |
| `fases` | enum 1 (mono), 2 (bi), 3 (tri) | sim | 1 | |
| `potenciaW` | decimal > 0 | sim | — | potência total do circuito em W |
| `fatorPotencia` | decimal (0,1] | sim | 0.92 | planilha usa 1.0 p/ iluminação e quadros, 0.92 caso contrário |
| `comprimentoM` | decimal > 0 | sim | — | comprimento do fio |
| `circuitosAgrupados` | int ≥ 1 | sim | 1 | nº de circuitos no mesmo eletroduto/bandeja |
| `temperaturaC` | int | sim | 30 | temperatura ambiente |
| `metodoInstalacao` | enum A1, A2, B1, B2, C, D, E, F, G | não | B1 | avançado; B1 = eletroduto embutido em alvenaria |
| `isolante` | enum `PVC` \| `EPR` | não | PVC | PVC 70 °C / EPR-XLPE 90 °C |
| `formaAgrupamento` | ref. 1–5 da tabela `agrupamento.json` | não | ref. 1 (em feixe / conduto fechado) | avançado |
| `fatorDemanda` | decimal (0,1] | não | 1.0 | por circuito |
| `quedaAdmissivelPct` | decimal | não | 4.0 | NBR 5410: circuitos terminais |
| `linhaSubterranea` | bool | não | false | muda a tabela de correção de temperatura (solo × ambiente) |

### 1.2 Quadro (alimentador geral)

| Campo | Padrão | Observações |
|---|---|---|
| `nome` / `local` | — | ex.: "QDC Bloco D — Dept. Engenharia Elétrica" |
| `sistemaTensao` | `V127_220` | enum `V127_220`, `V220_380`, `V254_440` (F-N / F-F) |
| `fasesAlimentador` | 3 | 1, 2 ou 3 |
| `fatorDemanda` | 1.0 | aplicado ao total do quadro |
| `cargaReservaVA` | 0 | planilha: 1000 VA por circuito "RESERVA" |
| `comprimentoM` | — | comprimento do alimentador |
| `metodoInstalacao` | B1 | do alimentador (tabela 90 °C) |
| `quedaAdmissivelPct` | 2.0 | NBR 5410: alimentação |
| `temperaturaC` | 30 | correção de temperatura do alimentador |

---

## 2. Cálculo por circuito

Notação: `P` = potenciaW, `FP` = fatorPotencia, `V` = tensaoV, `L` = comprimentoM, `e%` = quedaAdmissivelPct.

### 2.1 Potência aparente

```
S (VA) = P / FP                        # planilha AR: ROUND(P/FP, 0) — arredondar p/ inteiro
```

### 2.2 Corrente de projeto (Ip)

```
fases = 1:  Ip = S / V                 # V fase-neutro
fases = 2:  Ip = S / V                 # V fase-fase (2 condutores carregados)
fases = 3:  Ip = S / (√3 × V)          # V fase-fase
```
Arredondar para 1 casa decimal (planilha BF: `ROUND(...,1)`).

Conferência (Glide): 1419 W, FP 0,92, 220 V, 1F → S = 1542 VA, Ip = 7,0 A ✓; 5900 W, FP 1, 220 V, 1F → 26,8 A ✓.

### 2.3 Fatores de correção

```
fAgrup = agrupamento.json[formaAgrupamento][circuitosAgrupados]     # NBR 5410 Tab. 42
fTemp  = temperatura.json[ambiente|solo][temperaturaC][isolante]    # NBR 5410 Tab. 40
fTotal = fAgrup × fTemp
```
Temperatura fora da tabela → erro de validação ("temperatura sem fator de correção na NBR 5410").

### 2.4 Corrente corrigida (Ic)

```
Ic = (Ip × fatorDemanda) / fTotal      # planilha BG = (BF×AX)/(BD×BA)
```

### 2.5 Disjuntor recomendado

```
In = menor corrente nominal padrão (disjuntores.json) ≥ Ic
polos = fases (1P, 2P, 3P)
```
Sem valor padrão suficiente → erro "corrente acima do maior disjuntor da tabela".
A planilha (linha 5) usa Ic; linhas ≥ 6 usam Ip por erro de cópia — adotamos **Ic**.
Os mínimos por categoria da planilha (iluminação 16 A, tomadas 20 A, ar-cond. 25 A) valem para o modelo
de colunas da planilha, **não** para as categorias do Rodrigo — não aplicar (o Glide também não aplica:
Freezers/Força recebeu 10 A).

### 2.6 Seção por sobrecorrente (capacidade de condução)

Critério de coordenação da NBR 5410 (Ip ≤ In ≤ Iz'):

```
condutoresCarregados = 2 se fases ∈ {1, 2}; 3 se fases = 3
Iz'(S) = capacidade(cap-conducao-{70|90}.json, metodoInstalacao, condutoresCarregados, S) × fTotal
S_sobrecorrente = menor seção comercial com Iz'(S) ≥ In
```

### 2.7 Seção por queda de tensão

Fórmula simplificada da planilha (BU), condutividade do cobre 58 m/(Ω·mm²) para circuitos:

```
fases 1 ou 2:  S_calc = (2  × L × Ic × 100) / (58 × e% × V)
fases 3:       S_calc = (√3 × L × Ic × 100) / (58 × e% × V)
S_queda = menor seção comercial ≥ S_calc
```
Conferência (Glide, Freezers): (2×19×7×100)/(58×4×220) = 0,52 mm² → 0,75 mm² comercial ✓.

### 2.8 Seção mínima normativa (NBR 5410 Tabela 47 — imagem enviada)

```
ILUMINACAO  → 1,5 mm²
FORCA       → 2,5 mm²  (tomadas = força)
SINALIZACAO → 0,5 mm²
```

### 2.9 Seção final e condutores

```
S_final = max(S_sobrecorrente, S_queda, S_minima)
neutro  = neutro.json[S_final]          # NBR 5410 Tab. 48
terra   = terra.json[S_final]           # NBR 5410 Tab. 58 (PE)
```

Rótulo do cabo: `"{fases}F#{S_final}mm²+N{neutro}mm²+T{terra}mm²"` (sem neutro quando não aplicável — v1 mantém
sempre, como a planilha).

Queda de tensão efetiva com a seção final (informativo):
`quedaCalculadaPct = (2|√3 × L × Ic × 100) / (58 × S_final × V)`.

### 2.10 Saída por circuito

`ip`, `ic`, `fAgrup`, `fTemp`, `fTotal`, `disjuntor {polos, In}`, `sSobrecorrente`, `sQuedaCalculada`,
`sQueda`, `sMinima`, `sFinal`, `neutro`, `terra`, `rotuloCabo`, `quedaCalculadaPct`, `sVA`.

---

## 3. Cálculo do alimentador (por quadro)

Notação: `Vff` = tensão F-F do sistema (220, 380 ou 440), `Vfn` = F-N (127, 220, 254), `F` = fasesAlimentador.

### 3.1 Totais

```
P_total (W)   = Σ potenciaW dos circuitos
S_total (VA)  = Σ S dos circuitos
S_demandada   = (S_total + cargaReservaVA) × fatorDemanda        # planilha BP107
```

### 3.2 Corrente total

```
F = 1:  I_total = S_demandada / Vfn
F = 2:  I_total = S_demandada / Vff
F = 3:  I_total = S_demandada / (√3 × Vff)                        # planilha: S/381 no sistema 127/220
```
Conferência (Glide): 7,3 kVA / 220 (1F) = 33,3 A ✓.

### 3.3 Disjuntor geral

```
In_calc  = menor In padrão com 0,8 × In ≥ I_total     # regra dos 80% da planilha (DISJUNTORES col. B: BI105 = VLOOKUP aprox.)
In_min   = próximo In padrão ACIMA do maior In entre os circuitos     # regra do Rodrigo (mínimo)
In_geral = max(In_calc, In_min)
```
A planilha reserva 20% de folga no disjuntor **geral** (uso contínuo); os disjuntores dos circuitos
não têm essa folga (BI5 usa ≥ direto) — o sistema segue os dois comportamentos.
Conferência (Glide): I_total 33,3 → 0,8×50 = 40 ≥ 33,3 → 50 A; maior circuito 40 A → próximo padrão
50 A; geral = **50 A** ✓.

### 3.4 Cabo alimentador

Alimentador usa isolação EPR/XLPE 90 °C (nota da planilha, linha 106) e condutividade 56 m/(Ω·mm²):

```
S_amp    = menor seção com Iz'(cap-conducao-90, metodoInstalacao, F condutores, S) × fTotal ≥ In_geral, mínimo 2,5
S_minReg = próxima seção comercial ACIMA da maior S_final entre os circuitos   # regra do Rodrigo (mínimo)
S_base   = max(S_amp, S_minReg)
# verificação iterativa de queda (planilha BJ107..BL110):
enquanto queda%(S) = (2|√3 × L × I_total × 100)/(56 × S × V) > quedaAdmissivelPct: S = próxima seção
S_alimentador = resultado; reportar quedaCalculadaPct final
```
Conferência (Glide): maior circuito 6 mm² → próxima 10 mm²; alimentador = **10 mm²** ✓.

```
neutro/terra do alimentador: mesmas tabelas 48/58 sobre S_alimentador
```

### 3.5 Informativos do quadro

```
capacidadeQuadroVA = (√3 × Vff | Vff | Vfn, conforme F) × In_geral × 0.8      # planilha BP2: 381×In×0,8
```

### 3.6 Fora do escopo v1 (anotado para o futuro)

- Distribuição/equilíbrio de fases (colunas BJ:BO e desvio entre fases da planilha);
- Abas AWG, BARRAMENTO, EQUIPAMENTOS, FIOS POR TUBO, CAIXA DE PASSAGEM, ÁREA DO CABO 90 °C;
- Cadastro de cargas por catálogo de equipamentos (colunas B:AO da planilha — o app recebe potência total).

---

## 4. Listas de valores comerciais

- **Seções (mm²)**: coluna A das abas de capacidade — 0,5; 0,75; 1; 1,5; 2,5; 4; 6; 10; 16; 25; 35; 50; 70; 95; 120; 150; 185; 240; 300; ... (usar exatamente `cap-conducao-*.json → secoesMm2`).
- **Disjuntores (A)**: `disjuntores.json → correntesNominaisPadrao`.

"Próximo valor padrão acima de X" = menor elemento da lista **estritamente maior** que X.

## 5. Arredondamento e exibição

- `S` (VA): inteiro; `Ip`/`Ic`: 1 casa; seções: valor comercial exato; queda calculada: 2 casas.
- Cálculo interno em `double`/`BigDecimal` sem arredondar entre etapas; arredondar só na exibição/DTO.

## 6. Validações que geram erro (HTTP 422)

- Temperatura sem fator na tabela (ex.: PVC acima de 55 °C não subterrânea);
- Corrente corrigida acima do maior disjuntor/última seção das tabelas;
- FP fora de (0,1]; potência/comprimento ≤ 0; número de circuito duplicado no quadro.

## 7. Divergências conscientes em relação à planilha

| # | Planilha | Sistema | Motivo |
|---|---|---|---|
| 1 | Linhas ≥ 6 dimensionam disjuntor por Ip (`BF`) | Ic (`BG`), como a linha 5 | erro de cópia na planilha; norma exige In ≥ Ic |
| 2 | `BR5` usa `CONTARVALORES` (#NAME?) | max(sobrecorrente, queda, mínima Tab. 47) | função inexistente; intenção clara |
| 3 | Mínimos de disjuntor 16/20/25 A por coluna de carga | não aplicados | modelo de categorias diferente (§2.5) |
| 4 | `BE` bifásico no sistema 220/380 = `ROUND(220√3)-1` = 380 | tensão F-F nominal do sistema | mesmo resultado, sem gambiarra de arredondamento |
| 5 | Alimentador: VLOOKUP aproximado com `In×1,1` | menor seção com Iz' ≥ In_geral | VLOOKUP aprox. pode subdimensionar; critério da norma |
| 6 | Neutro/terra dos circuitos travados em `$BR$5` | por circuito | erro de referência absoluta na planilha |
| 7 | CAP 70º: B2/2c @35 mm² = 11 A; D/2c @70 mm² = 193 A; B1/2c @630 mm² = 458 A | 111 / 183 / 749 A | erros de digitação vs NBR 5410 Tab. 36 (achados na verificação da extração; ver `correcoes` nos JSONs) |
| 8 | TERRA: fase 150→70, 400→185, 500→240, 630→300 | 95 / 240 / 300 / 400 mm² | abaixo do mínimo S/2 da Tab. 58 arredondado à seção normalizada superior |
