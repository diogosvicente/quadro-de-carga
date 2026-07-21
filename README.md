# Dimensionamento de Circuitos — NBR 5410 (UERJ)

Sistema web para a equipe do Departamento de Engenharia Elétrica cadastrar circuitos e obter o
dimensionamento conforme NBR 5410: corrente de projeto e corrigida, disjuntor, seção do cabo
(por sobrecorrente, queda de tensão e mínima normativa), neutro/terra e o alimentador geral do
quadro — substituindo a planilha `docs/iniciais/QUADRO DE CARGA_*.xlsx`.

**Stack**: Java 21 + Spring Boot 3 (API REST) · React 18 + TypeScript (Vite) · PostgreSQL 16 · Docker Compose.

## Rodar

```bash
docker compose up -d --build
```

- App: http://localhost:8085
- API + Swagger: http://localhost:8086/swagger-ui.html
- PostgreSQL: localhost:5433 (eletrica/eletrica)

O banco já sobe migrado (Flyway) com um quadro de exemplo contendo os dois circuitos do teste
feito no Glide.

## Desenvolvimento

```bash
# backend (testes, sem Java no host):
docker run --rm -v $PWD/backend:/app -w /app -v eletrica-m2:/root/.m2 maven:3-eclipse-temurin-21 mvn test

# frontend com hot-reload (proxy /api → localhost:8086):
cd frontend && npm install && npm run dev
```

## Documentação

| Arquivo | Conteúdo |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | camadas, padrões de código, checklist de feature |
| [docs/CALCULOS.md](docs/CALCULOS.md) | especificação das fórmulas (fonte da verdade do motor de cálculo) |
| [docs/dados-normativos/](docs/dados-normativos/) | tabelas da NBR 5410 extraídas da planilha (JSON verificado) |

> ⚠️ As tabelas extraídas corrigem erros de digitação encontrados na planilha original
> (capacidades B2 35 mm², D 70 mm², B1 630 mm² e quatro linhas da tabela de terra) — as correções
> estão listadas no campo `correcoes` de cada JSON e em `docs/CALCULOS.md` §7.
