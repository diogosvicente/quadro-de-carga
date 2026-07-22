# Contexto do Projeto — para retomar em outra conversa

> Documento de *handoff*: leia isto primeiro para entender o projeto, as decisões já
> tomadas, o estado atual e as pendências, evitando refazer trabalho ou repetir erros.
> Fontes canônicas complementares: [`ARCHITECTURE.md`](../ARCHITECTURE.md) (stack, camadas,
> padrões, design system) e [`docs/CALCULOS.md`](CALCULOS.md) (fórmulas — fonte da verdade
> do motor de cálculo). Tabelas normativas verificadas em [`docs/dados-normativos/`](dados-normativos/).

---

## 1. O que é o projeto

Sistema web para **dimensionamento de circuitos elétricos conforme a NBR 5410**, para o
**Departamento de Engenharia Elétrica da UERJ**. Substitui uma planilha Excel
(`docs/iniciais/QUADRO DE CARGA_teste_queda de tensão_rev12.xlsx`) e um protótipo no-code (Glide).

Fluxo do usuário (3 telas dentro de um quadro):
1. **Novo Circuito** — cadastra cada circuito (potência W, fator de potência, tensão, fases, comprimento, etc.).
2. **Circuitos** — lista dos circuitos cadastrados.
3. **Quadro Elétrico** (antes "Resumo") — o resultado final: **disjuntor geral + cabo alimentador** + tabela dos circuitos (quadro de cargas). É a partir daqui que eles montam os quadros elétricos físicos do corredor.

## 2. Pessoas

- **Diogo** (usuário desta conversa): trabalha na **DGTI/UERJ**, é quem desenvolve/opera o sistema. Fala português. Prefere entregas rodando via `docker compose` e arquitetura documentada. Mantém também o e-Prefeitura (PHP/CI4/MariaDB), cujo ARCHITECTURE.md foi o molde do deste projeto.
- **Rodrigo Vieira** (cliente/usuário final): **engenheiro eletricista da UERJ**, amigo do Diogo. Define os requisitos por WhatsApp; o Diogo repassa. É quem testa o app.

### Modelo mental do Rodrigo (importante para não errar requisito)
- O cadastro é **por circuito**; cada circuito tem **Potência (W)** e **Fator de Potência** editáveis.
- **"O quadro é o resumo final"**, com disjuntor geral e cabo alimentador — por isso a tela final se chama **"Quadro Elétrico"** e a criação de um quadro pede **só o nome** (parâmetros do alimentador são ajustados depois, na tela do Quadro Elétrico).
- As imagens do Glide que ele mandou são **meramente ilustrativas** — não precisam ser copiadas.

## 3. Stack e como rodar

| Camada | Tecnologia |
|---|---|
| Backend | Java 21 + Spring Boot 3.3 (REST), Flyway, springdoc/Swagger |
| Cálculo | pacote `calc/` **puro** (sem Spring/JPA), tabelas NBR 5410 em `resources/nbr5410/*.json` |
| Banco | PostgreSQL 16 (Flyway migra na subida; seed com 1 quadro exemplo) |
| Frontend | React 18 + TypeScript (Vite) + **Mantine 7** (design system), Inter, Tabler icons |
| Infra | Docker Compose: `db`, `api`, `web` (nginx serve o build do Vite + proxy `/api`) |

```bash
cd /home/diogo/rodrigo
docker compose up -d --build           # sobe tudo
# web/app:  http://localhost:8085
# API+Swagger: http://localhost:8086/swagger-ui.html
# Postgres: localhost:5433  (db eletrica / user eletrica / senha eletrica)
```

- **Não há Java/Maven no host** — build/testes do backend via container:
  `docker run --rm -v $PWD/backend:/app -w /app -v eletrica-m2:/root/.m2 maven:3-eclipse-temurin-21 mvn test`
- Frontend dev: `cd frontend && npm install && npm run dev` (proxy `/api` → localhost:8086).
- **Nome do arquivo é `docker-compose.yml`**, mas sempre use o **comando `docker compose`** (com espaço), nunca `docker-compose`.

### Portas dos OUTROS projetos do Diogo (NÃO conflitar)
e-prefeitura: 80, 8081 · gestao-de-projetos-servpen: 5432 · cic-backend: 8200, 9200 ·
e-uerj-backend: 8100, 9100 · e-uerj-frontend: 3000, 3001, 5000.
As deste projeto (8085, 8086, 5433) já foram escolhidas para não colidir.

## 4. Domínio / cálculo (resumo — detalhe em docs/CALCULOS.md)

- **Por circuito**: potência aparente → corrente de projeto (Ip) → fatores de correção (agrupamento Tab.42, temperatura Tab.40) → corrente corrigida (Ic) → **disjuntor** (menor padrão ≥ Ic) → **seção** = máx(sobrecorrente, queda de tensão, mínima Tab.47) → neutro (Tab.48) e terra (Tab.58).
- **Alimentador (quadro)**: soma das cargas × fator de demanda → corrente total → disjuntor geral (regra dos 80% da planilha) e cabo (EPR 90 °C, verificação iterativa de queda). **Regra do Rodrigo**: disjuntor geral e cabo do alimentador são, no mínimo, **um valor padrão acima do maior circuito**.
- **Valores dourados** validados em testes (17 testes, verdes): circuitos "Freezers" e "Quadro de comando" batem com o app Glide; linha 5 da planilha bate com valores cacheados.

### Divergências conscientes vs. planilha (CALCULOS.md §7) e correções
A planilha original do Rodrigo tem **erros** (corrigidos na extração, campo `correcoes` nos JSONs):
capacidade de condução B2 35 mm² (11→**111 A**), D 70 mm² (193→**183 A**), B1 630 mm² (458→**749 A**),
e 4 linhas da tabela TERRA. **Pendência: avisar o Rodrigo para corrigir a planilha dele.**
Também: dimensionamos disjuntor por Ic (não Ip), seção final por máx dos três critérios, etc.

## 5. Estado atual (o que está pronto e verificado)

- Backend completo (domínio, cálculo, REST, validações, `ApiExceptionHandler` com envelope `{status, mensagem, erros}`), 17 testes verdes.
- Frontend completo, **migrado para Mantine 7** (tema azul de engenharia, dark mode, mobile-first com nav inferior no celular). Build `tsc` estrito + `vite` limpo. Verificado por screenshots headless (Playwright via imagem Docker) — renderiza em claro/escuro/mobile, **zero erros de console**.
- Sistema **rodando** nos 3 contêineres; smoke-tests OK (app, API, resumo).
- Feedback do Rodrigo já aplicado: renome "Resumo"→**"Quadro Elétrico"**, criação de quadro **só com nome** (`QuadroForm` tem prop `variante: 'simples' | 'completo'`).

## 6. Demo externa (túnel) — como funciona

Para o Rodrigo testar de fora, expõe-se o app (porta 8085) por um **Cloudflare quick tunnel**.
Há um script de gerência: **[`scripts/tunel.sh`](../scripts/tunel.sh)** (`start|stop|restart|status|link|logs`).

```bash
cd ~/rodrigo && ./scripts/tunel.sh start   # sobe e espera o link ficar acessível; imprime a URL
./scripts/tunel.sh stop                     # derruba
```

- `cloudflared` está em `~/.local/bin/cloudflared`. O `start` roda com `nohup` (sobrevive a fechar o terminal).
- **O link muda a cada `start`** (quick tunnel = subdomínio aleatório `*.trycloudflare.com`; leva ~30–60s para o edge rotear). Para link fixo, seria preciso um *named tunnel* (conta Cloudflare) — ainda não feito.
- **Rede da UERJ permite o túnel** (saída 443 liberada; testado). O túnel é conexão **de saída** — não abre porta de entrada no firewall.
- **Só a porta 8085 é tunelada**; banco (5433) e API direta/Swagger (8086) **não** ficam expostos.
- **ngrok não funcionou**: a rede permite, mas o token da conta do Rodrigo foi **recusado** pelo servidor (ERR_NGROK_4018, provável e-mail não verificado). Por isso adotou-se o Cloudflare.

### Segurança (decisão consciente)
O app **não tem autenticação** (v1). Enquanto o túnel está no ar, qualquer um com o link vê/edita os
dados de demonstração — aceitável para demo com dados descartáveis. **Não** compromete a rede interna:
o app é isolado (contêineres próprios + só o próprio Postgres), sem SSRF/pivô. Higiene: derrubar o túnel
fora da demo. Se um dia virar ferramenta interna real → adicionar login e apertar CORS (ver §7).

## 7. Fix recente — 403 ao editar/cadastrar pelo túnel (CORS)

**Sintoma**: pelo túnel, carregar/ver funcionava, mas **salvar/cadastrar/excluir dava 403**.
**Causa**: CORS do backend só permitia `localhost:5173` (dev server). Requisições mutantes (POST/PUT/DELETE)
levam header `Origin` mesmo same-origin; atrás do nginx/Cloudflare o Spring as via como cross-origin e barrava.
GET de carregamento não manda `Origin` → passava.
**Correção**: `CorsConfig.java` agora usa `allowedOriginPatterns("*")` (seguro: API sem auth/cookies).
Verificado local e ponta a ponta por túnel real (editar circuito 1 → 200). Deploy: só o container `api` reconstruído.
**Nota futura**: como front e API são **same-origin** via nginx, o ideal quando houver auth é dispensar CORS
(ou allow-list específica) — o CORS só existia por causa do dev server.

## 8. Pendências / próximos passos

- [ ] **Commit + push** de tudo que está aplicado mas não commitado: (a) feedback "Quadro Elétrico" + criação simplificada; (b) migração Mantine + doc §4.11; (c) `scripts/tunel.sh`; (d) fix de CORS; (e) este `CONTEXTO.md`. Repo: `github.com/diogosvicente/quadro-de-carga` (SSH configurado). **Commits SEM trailer "Co-Authored-By: Claude"** (pedido do Diogo). Commitar **só quando o Diogo pedir**.
- [ ] **Avisar o Rodrigo** dos erros de digitação na planilha original (§4).
- [ ] Opcional: *named tunnel* Cloudflare para link fixo; autenticação/perfis; exportar PDF do quadro; equilíbrio de fases (fora do escopo v1, ver CALCULOS.md §3.6).
- Aviso menor: `npm audit` acusa 2 vulnerabilidades (esbuild via vite) **dev-only** — não afetam produção; fix exige `vite@8` (breaking), não urgente.

## 9. Gotchas do ambiente (para não perder tempo)

- **Sem `sudo` sem senha**; **sem navegador no host** (Chromium precisa de libs ausentes) — para screenshots, usei a imagem `mcr.microsoft.com/playwright` com `--network host` reaproveitando o Chromium baixado.
- `pkill`/`pgrep` às vezes retornam **exit 144** no sandbox (ruído; a ação funciona) — evitar depender do código de saída deles.
- Git: branch `main`; usuário "Diogo Vicente". Working dir: `/home/diogo/rodrigo`.
- Idioma do domínio em **português** (sem acento em identificadores: `tensaoV`, `secaoFinalMm2`).
