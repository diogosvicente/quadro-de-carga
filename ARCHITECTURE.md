# Contexto do Projeto — Dimensionamento de Circuitos NBR 5410 (UERJ)

> Documento de referência sobre a stack, arquitetura e padrão de criação de novas features.
> Use este guia sempre que for implementar uma nova funcionalidade no projeto.
> A especificação do domínio (fórmulas e tabelas) está em `docs/CALCULOS.md` e `docs/dados-normativos/`.

---

## 1. Stack Tecnológica

### Backend (`backend/`)
| Camada | Tecnologia | Versão |
|---|---|---|
| Linguagem | Java | 21 (LTS) |
| Framework | **Spring Boot** | 3.3.x |
| Persistência | Spring Data JPA (Hibernate) | via starter |
| Banco de dados | **PostgreSQL 16** | driver `org.postgresql` |
| Migrations | **Flyway** | `V###__descricao.sql` em `src/main/resources/db/migration` |
| Validação | Jakarta Bean Validation (`spring-boot-starter-validation`) | — |
| API Docs | springdoc-openapi (`/swagger-ui.html`) | 2.x |
| Testes | JUnit 5 + Spring Boot Test + Testcontainers (opcional) | — |
| Build | Maven (via Docker `maven:3-eclipse-temurin-21` — não requer Java no host) | — |

### Frontend (`frontend/`)
| Tecnologia | Uso |
|---|---|
| React 18 + TypeScript | SPA |
| Vite | build/dev server (proxy `/api` → backend em dev) |
| React Router 6 | rotas |
| CSS puro (`src/styles.css`, variáveis CSS) | tema simples, mobile-first — **sem** framework CSS |
| `fetch` tipado em `src/api/` | **nunca** chamar API direto de componente |

### Infraestrutura
- **Docker Compose** com 3 serviços: `db` (postgres:16-alpine), `api` (Spring Boot), `web` (nginx servindo o build do Vite e fazendo proxy de `/api` para `api:8080`).
- Porta 8085 → web (app completo), 8086 → api direta (Swagger).
- Volumes: `pgdata` para o banco. Sem bind-mounts de código em produção.

---

## 2. Estrutura de Pastas

```
backend/src/main/java/br/uerj/eletrica/
├── EletricaApplication.java
├── config/            CorsConfig, OpenApiConfig, etc.
├── domain/            Entidades JPA + enums do domínio (TipoCircuito, Fases, ...)
├── dto/               Records de entrada/saída da API, organizados por feature
├── repository/        Interfaces Spring Data JPA (1 por entidade)
├── service/           Regras de negócio; orquestram repositórios e o motor de cálculo
├── calc/              Motor de cálculo NBR 5410 — PURO (sem Spring, sem JPA)
│   └── tabelas/       Loader + records das tabelas normativas (JSON em resources)
└── web/               Controllers REST (thin) + ApiExceptionHandler

backend/src/main/resources/
├── application.yml
├── db/migration/      Flyway: V001__create_quadro.sql, V002__create_circuito.sql, ...
└── nbr5410/           JSONs das tabelas normativas (copiados de docs/dados-normativos)

frontend/src/
├── api/               cliente HTTP tipado (client.ts, quadros.ts, circuitos.ts)
├── types/             tipos TS espelhando os DTOs do backend
├── components/        componentes reutilizáveis (Campo, Tabela, StatTile, ...)
├── pages/             1 pasta por tela (NovoCircuito/, Circuitos/, DetalheCircuito/, Resumo/, Quadros/)
├── App.tsx            rotas + layout (navegação inferior no mobile)
└── styles.css         variáveis de tema + estilos globais
```

### Organização por feature

Uma feature "Circuito" toca, no máximo:

```
backend: domain/Circuito.java, repository/CircuitoRepository.java,
         dto/circuito/{CircuitoRequest,CircuitoResponse,ResultadoCircuito}.java,
         service/CircuitoService.java, web/CircuitoController.java,
         db/migration/V00X__....sql
frontend: types/circuito.ts, api/circuitos.ts, pages/NovoCircuito/, pages/Circuitos/
```

---

## 3. Arquitetura em Camadas

```
Request HTTP (POST /api/quadros/{id}/circuitos)
      │
      ▼
┌──────────────────┐   @Valid — Bean Validation valida formato/faixas
│  Controller      │   thin: recebe DTO, delega ao Service, devolve DTO
└──────┬───────────┘
       ▼
┌──────────────────┐   regras de negócio (unicidade do número no quadro,
│  Service         │   existência do quadro), @Transactional,
└──┬────────────┬──┘   monta entidade ↔ DTO
   │            │
   ▼            ▼
┌─────────┐  ┌──────────────────┐   PURO: função (entradas → resultados).
│Repository│  │  calc/ (engine)  │   Sem Spring, sem banco, sem I/O.
│(JPA)     │  │  NBR 5410        │   Tabelas normativas carregadas 1x de
└────┬─────┘  └──────────────────┘   resources/nbr5410/*.json.
     ▼
┌─────────┐
│PostgreSQL│
└─────────┘
```

### Regra de ouro

> **Controller** = thin: `DTO → Service → DTO`. Nunca lógica de negócio nem cálculo.
> **Service** = regra de negócio + transação + conversão entidade↔DTO.
> **Repository** = interface Spring Data; métodos derivados ou `@Query`. Um por entidade.
> **calc/** = motor de cálculo **puro e determinístico**. Toda fórmula do `docs/CALCULOS.md` vive aqui
> e **somente** aqui. É testável sem subir contexto Spring.
> **DTO** = `record` Java imutável. Entidade JPA **nunca** sai pela API.
> **Resultados de cálculo não são persistidos** — são derivados das entradas a cada leitura.
> Persistir só o que o usuário digitou.

---

## 4. Padrões de Código (templates prontos)

### 4.1 Migration (Flyway)

**Arquivo:** `backend/src/main/resources/db/migration/V003__create_feature.sql`

```sql
CREATE TABLE feature (
    id          BIGSERIAL PRIMARY KEY,
    quadro_id   BIGINT      NOT NULL REFERENCES quadro (id) ON DELETE CASCADE,
    nome        VARCHAR(120) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_feature_quadro_nome ON feature (quadro_id, nome);
```

Regras: nomes de tabela/coluna em `snake_case` singular; toda FK com `ON DELETE` explícito;
constraints de unicidade no banco (não só no Service); nunca editar migration já aplicada — criar a próxima.

### 4.2 Entidade JPA

```java
@Entity
@Table(name = "circuito")
public class Circuito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quadro_id")
    private Quadro quadro;

    @Column(nullable = false)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCircuito tipo;

    // getters/setters; SEM lógica de cálculo na entidade
}
```

Regras: enums sempre `EnumType.STRING`; associações `LAZY`; sem Lombok (o projeto não usa);
a entidade guarda **entradas**, nunca resultados calculados.

### 4.3 DTOs (records)

```java
public record CircuitoRequest(
    @NotNull @Positive Integer numero,
    @Size(max = 120) String descricao,
    @NotNull TipoCircuito tipo,
    @NotNull Integer tensaoV,
    @NotNull @Min(1) @Max(3) Integer fases,
    @NotNull @Positive BigDecimal potenciaW,
    @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("1") BigDecimal fatorPotencia,
    ...
) {}

public record CircuitoResponse(Long id, Integer numero, ..., ResultadoCircuito resultado) {}
```

Regras: entrada e saída são records distintos; validação de formato/faixa por annotations no request
(o Service valida apenas regra de negócio); saída sempre inclui o objeto `resultado` calculado.

### 4.4 Repository

```java
public interface CircuitoRepository extends JpaRepository<Circuito, Long> {
    List<Circuito> findByQuadroIdOrderByNumero(Long quadroId);
    boolean existsByQuadroIdAndNumero(Long quadroId, Integer numero);
    Optional<Circuito> findByIdAndQuadroId(Long id, Long quadroId);
}
```

Preferir métodos derivados; `@Query` só quando o nome derivado ficar ilegível.

### 4.5 Service

```java
@Service
public class CircuitoService {

    private final CircuitoRepository circuitos;
    private final QuadroRepository quadros;
    private final CalculadoraCircuito calculadora;   // injeção por construtor, sempre

    public CircuitoService(CircuitoRepository circuitos, QuadroRepository quadros,
                           CalculadoraCircuito calculadora) { ... }

    @Transactional
    public CircuitoResponse criar(Long quadroId, CircuitoRequest req) {
        Quadro quadro = quadros.findById(quadroId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Quadro não encontrado"));
        if (circuitos.existsByQuadroIdAndNumero(quadroId, req.numero())) {
            throw new RegraDeNegocioException("Já existe o circuito nº " + req.numero() + " neste quadro.");
        }
        Circuito c = toEntity(req, quadro);
        circuitos.save(c);
        return toResponse(c);                 // calcula via calculadora ao montar o response
    }
}
```

Regras: injeção **por construtor** (nunca `@Autowired` em campo, nunca `new` de bean);
exceções de negócio próprias (`RegraDeNegocioException` → 422, `RecursoNaoEncontradoException` → 404);
`@Transactional` no método de escrita, não na classe.

### 4.6 Motor de cálculo (calc/)

```java
public final class CalculadoraCircuito {
    private final TabelasNbr5410 tabelas;   // carregadas 1x de resources/nbr5410/*.json

    public ResultadoCircuito calcular(EntradaCircuito entrada) { ... }
}
```

Regras: classes do `calc/` **não** conhecem Spring, JPA ou DTOs da web — recebem/retornam records
próprios (`EntradaCircuito`, `ResultadoCircuito`); qualquer mudança de fórmula exige atualizar
`docs/CALCULOS.md` no mesmo commit; todo comportamento coberto por teste de unidade com valores
dourados (planilha/Glide — ver `CalculadoraCircuitoTest`).

### 4.7 Controller (thin)

```java
@RestController
@RequestMapping("/api/quadros/{quadroId}/circuitos")
public class CircuitoController {

    private final CircuitoService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CircuitoResponse criar(@PathVariable Long quadroId, @Valid @RequestBody CircuitoRequest req) {
        return service.criar(quadroId, req);
    }
}
```

Erros são tratados **somente** no `web/ApiExceptionHandler` (`@RestControllerAdvice`), que devolve
sempre o mesmo envelope: `{ "status": 422, "mensagem": "...", "erros": { "campo": "detalhe" } }`.

### 4.8 Rotas da API

| Método | Rota | Ação |
|---|---|---|
| GET | `/api/quadros` | lista quadros (com totais resumidos) |
| POST | `/api/quadros` | cria quadro |
| GET/PUT/DELETE | `/api/quadros/{id}` | detalhe/edita/remove |
| GET | `/api/quadros/{id}/resumo` | resumo: alimentador geral + circuitos calculados |
| GET/POST | `/api/quadros/{id}/circuitos` | lista/cria circuito |
| GET/PUT/DELETE | `/api/quadros/{id}/circuitos/{cid}` | detalhe (com resultado)/edita/remove |
| POST | `/api/calculos/circuito` | cálculo avulso sem persistir (preview do formulário) |
| GET | `/api/referencias` | enums + listas p/ selects (tensões, métodos, tabelas) |

Convenções: URLs em kebab/minúsculo, recursos no plural; corpo e respostas em `camelCase` pt-BR
(`potenciaW`, `fatorPotencia`); `DELETE` responde 204; validação de formato → 400, regra de negócio → 422.

### 4.9 Frontend — página

```
pages/NovoCircuito/
└── NovoCircuito.tsx     estado do form + submit; usa api/circuitos.ts
```

Regras: componente de página busca dados via `src/api/*` e trata os 3 estados (carregando / erro /
dado); tipos vêm de `src/types/*` (espelham os DTOs — atualizar junto com o backend); formulários
controlados; mensagens de erro da API exibidas junto ao campo (`erros` do envelope) ou em alerta no
topo; unidades sempre visíveis nos rótulos ("Potência Total (W)").

### 4.10 Frontend — cliente de API

```ts
// api/client.ts — único lugar que conhece fetch/baseURL/envelope de erro
export async function api<T>(path: string, init?: RequestInit): Promise<T> { ... }

// api/circuitos.ts
export const criarCircuito = (quadroId: number, corpo: CircuitoRequest) =>
  api<CircuitoResponse>(`/api/quadros/${quadroId}/circuitos`, { method: 'POST', body: JSON.stringify(corpo) });
```

Componentes **nunca** usam `fetch` direto; toda chamada passa por `api/`.

---

## 5. Convenções de Nomenclatura

| Camada | Padrão | Exemplo |
|---|---|---|
| Entidade | substantivo singular | `Circuito`, `Quadro` |
| Repository | `<Entidade>Repository` | `CircuitoRepository` |
| Service | `<Entidade>Service`; métodos `criar/atualizar/remover/buscar/listar` | `CircuitoService.criar` |
| DTO | `<Entidade>Request` / `<Entidade>Response` / records de resultado | `CircuitoRequest` |
| Controller | `<Entidade>Controller` | `CircuitoController` |
| Cálculo | `Calculadora<Alvo>` + records `Entrada*`/`Resultado*` | `CalculadoraAlimentador` |
| Tabela DB | `snake_case` singular | `circuito`, `quadro` |
| Migration | `V###__verbo_alvo.sql` | `V002__create_circuito.sql` |
| Página React | pasta + componente `PascalCase` | `pages/Resumo/Resumo.tsx` |
| Arquivo TS util/api | `camelCase.ts` | `api/circuitos.ts` |

Idioma: domínio em **português** (sem acentos em identificadores: `tensaoV`, `secaoFinalMm2`);
termos técnicos de infraestrutura em inglês (`Repository`, `Request`).

---

## 6. Checklist para Criar uma Nova Feature

1. [ ] **Migration** Flyway nova (`V###__...sql`) — nunca editar as aplicadas
2. [ ] **Entidade** JPA (só entradas; enums STRING; associações LAZY)
3. [ ] **Repository** (métodos derivados)
4. [ ] **DTOs** records com Bean Validation no Request
5. [ ] **Service** (regras de negócio, @Transactional, exceções de negócio)
6. [ ] **Cálculo** — se envolver fórmula nova: atualizar `docs/CALCULOS.md` + `calc/` + teste dourado
7. [ ] **Controller** thin + rota na tabela da §4.8 deste documento
8. [ ] **Testes**: unidade do `calc/`; `@WebMvcTest` do controller ou teste de integração do fluxo
9. [ ] **Frontend**: `types/` → `api/` → página; tratar carregando/erro/vazio
10. [ ] **Swagger** conferido em `/swagger-ui.html` (a annotation `@Operation` é opcional, o contrato não)
11. [ ] **Build completo**: `docker compose build && docker compose up -d` e smoke-test das telas

---

## 7. Como rodar

```bash
# tudo (build + subir):
docker compose up -d --build          # web: http://localhost:8085  api/swagger: http://localhost:8086/swagger-ui.html

# desenvolvimento frontend com hot-reload:
cd frontend && npm install && npm run dev      # proxy /api → localhost:8086

# testes do backend (sem Java no host):
docker run --rm -v $PWD/backend:/app -w /app maven:3-eclipse-temurin-21 mvn test
```

Variáveis (ver `docker-compose.yml`): `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`. Flyway roda na subida.

---

## 8. Evoluções previstas (onde encaixar)

- **Autenticação/perfis** (equipe de campo × engenheiro): Spring Security + filtro JWT em
  `config/`; papéis por rota na §4.8. Não implementado na v1 (uso interno).
- **Auditoria de alterações**: entidade `audit_log` + `@EntityListeners` nas entidades; registrar
  quem/quando/antes/depois nas escritas.
- **Exportação PDF/Excel do quadro de cargas**: endpoint em `web/`, geração no Service.
- **Equilíbrio de fases e demais abas da planilha**: ver `docs/CALCULOS.md` §3.6.
