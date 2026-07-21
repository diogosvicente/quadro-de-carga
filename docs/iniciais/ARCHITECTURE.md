# Contexto do Projeto e-Prefeitura

> Documento de referência sobre a stack, arquitetura e padrão de criação de novas features.
> Use este guia sempre que for implementar uma nova funcionalidade no projeto.

---

## 1. Stack Tecnológica

### Backend
| Camada | Tecnologia | Versão |
|---|---|---|
| Linguagem | PHP | `^8.1` (rodando em `8.2` no container) |
| Framework | **CodeIgniter 4** | `^4.0` (atual: v4.7.0) |
| Banco de dados | **MariaDB 10.5** (driver MySQLi) | via `app/Config/Database.php` |
| PDF | mpdf/mpdf | `^8.2` |
| Realtime | pusher/pusher-php-server | `^7.2` |
| API Docs | zircote/swagger-php | `^6.0` |
| Testes | PHPUnit | `^10.5.16` |

### Frontend
| Tecnologia | Versão | Uso |
|---|---|---|
| Bootstrap 5 | — | Grid, componentes, modais |
| TailwindCSS | `3.4.17` | Classes utilitárias em views modernas |
| jQuery | — | Manipulação DOM clássica |
| Highcharts | — | Gráficos dashboards |
| FullCalendar | — | Calendário de reservas |
| DataTables | — | Tabelas CRUD admin |
| Bootstrap Icons | — | `bi bi-*` em views modernas |
| Font Awesome | — | `fa fa-*` em views legadas |
| **SweetAlert2** | v11 (**já no projeto**) | Confirmações e alertas — **nunca** `alert()`/`confirm()` nativos nem CDN externo (§4.11) |

### Infraestrutura
- **Docker Compose** com 3 containers: `apache` (web), `mariadb` (db), `phpmyadmin` (admin db)
- Porta 80 → Apache, Porta 8080 → phpMyAdmin
- Volume do projeto: `/var/www/html/e-prefeitura/`

---

## 2. Estrutura de Pastas

```
app/
├── Commands/          CLI commands (extends BaseCommand)
├── Config/            Routes.php, Database.php, Filters.php, Systems.php, etc.
├── Constants/         Constantes da aplicação
├── Controllers/       HTTP controllers, organizados por módulo
├── DTOs/              Data Transfer Objects, organizados por módulo/feature
├── Database/
│   ├── Migrations/    Schema DB (YYYY-MM-DD-HHMMSS_ClassName.php)
│   └── Seeds/         Dados iniciais
├── Entities/          Entidades (opcional — pouco usado)
├── Filters/           Middleware HTTP (auth, csrf, etc.)
├── Helpers/           Funções helper
├── Libraries/         Libs customizadas
├── Models/            Models de DB (SEM joins — uma tabela por model)
├── Repositories/      Queries com JOIN (SOMENTE quando necessário)
├── Services/          Business logic + CRUD auditado
├── Traits/            Traits reutilizáveis
├── Validation/        Regras de validação customizadas
└── Views/             Templates (organizados por módulo)
```

### Organização por módulo

Os módulos do projeto (pastas dentro de cada camada):

- **`e_Prefeitura`** — núcleo (campus, prédios, blocos, espaços, pavimentos, unidades, departamentos, papéis)
- **`e_EspacoFisico`** — reserva/agendamento de espaços físicos, recursos
- **`e_Transporte`** — condutores, veículos, agendamento, BDT
- **`e_Contratos`** — gestão de contratos
- **`e_Transparencia`** — portal da transparência
- **`e_Projetos`** — projetos

Exemplo de caminho para uma feature "Bloco" do módulo `e_Prefeitura`:

```
app/Controllers/e_Prefeitura/Admin/BlocoController.php
app/Services/e_Prefeitura/BlocoService.php
app/Models/e_Prefeitura/BlocoModel.php
app/DTOs/e_Prefeitura/Bloco/CreateBlocoDTO.php
app/DTOs/e_Prefeitura/Bloco/UpdateBlocoDTO.php
app/Views/e_Prefeitura/admin/bloco/index.php
app/Views/e_Prefeitura/admin/bloco/form.php
```

---

## 3. Arquitetura em Camadas

### Visão geral do fluxo

```
┌─────────────┐
│  Request    │  (HTTP POST /admin/bloco/store)
└──────┬──────┘
       │
       ▼
┌──────────────────┐
│  Controller      │  thin — apenas orquestra
│  (BlocoCtrl)     │
└──────┬───────────┘
       │  ::fromArray($post)
       ▼
┌──────────────────┐
│  DTO             │  sanitiza, trimma, converte tipos
│  (CreateBlocoDTO)│
└──────┬───────────┘
       │  $dto->toArray()
       ▼
┌──────────────────┐
│  Service         │  valida regras de negócio,
│  (BlocoService)  │  orquestra persistência auditada
└──┬───────────────┘
   │
   │  ┌─ query simples? ──────┐
   │  │                       ▼
   │  │              ┌─────────────────┐
   │  │              │  Model          │  CRUD 1 tabela
   │  │              │  (BlocoModel)   │  SEM joins
   │  │              └─────────────────┘
   │  │
   │  └─ query com JOIN? ─────┐
   │                          ▼
   │              ┌─────────────────┐
   │              │  Repository     │  queries complexas
   │              │  (EspacoRepo)   │  com JOINs
   │              └─────────────────┘
   │
   ▼
┌─────────────┐
│  DB         │
└─────────────┘
```

### Regra de ouro

> **Model** = 1 tabela, sem joins.
> **Repository** = JOINs e queries entre múltiplas tabelas.
> **Service** = lógica de negócio + validação + CRUD auditado.
> **Controller** = thin — só recebe request, chama DTO, delega ao Service, responde.
> **DTO** = contrato imutável de input.

**Sem Repository** → feature simples (ex.: Bloco). Model basta.
**Com Repository** → feature com JOINs (ex.: Espaço precisa listar junto com campus/prédio/bloco).

---

## 4. Padrões de Código (templates prontos)

### 4.1 Migration

**Arquivo:** `app/Database/Migrations/YYYY-MM-DD-HHMMSS_CreateFeatureTable.php`

```php
<?php

namespace App\Database\Migrations;

use CodeIgniter\Database\Migration;

class CreateFeatureTable extends Migration
{
    public function up()
    {
        $this->forge->addField([
            'id' => [
                'type'           => 'INT',
                'constraint'     => 11,
                'auto_increment' => true,
            ],
            'id_outro' => [
                'type'       => 'INT',
                'constraint' => 11,
                'null'       => true,
            ],
            'nome' => [
                'type'       => 'VARCHAR',
                'constraint' => 255,
                'null'       => false,
            ],
            'created_at' => ['type' => 'DATETIME', 'null' => true],
            'updated_at' => ['type' => 'DATETIME', 'null' => true],
            'deleted_at' => ['type' => 'DATETIME', 'null' => true], // se soft delete
        ]);

        $this->forge->addKey('id', true);
        $this->forge->addForeignKey('id_outro', 'outra_tabela', 'id', 'CASCADE', 'SET NULL', 'fk_feature_outro');
        $this->forge->createTable('nome_tabela');
    }

    public function down()
    {
        $this->forge->dropTable('nome_tabela', true);
    }
}
```

Rodar: `docker exec apache php /var/www/html/e-prefeitura/spark migrate`.

### 4.2 Model (1 tabela, SEM joins)

**Arquivo:** `app/Models/[Modulo]/FeatureModel.php`

```php
<?php

namespace App\Models\e_Prefeitura;

use CodeIgniter\Model;

class FeatureModel extends Model
{
    protected $table         = 'nome_tabela';
    protected $primaryKey    = 'id';
    protected $returnType    = 'object';
    protected $useSoftDeletes = false; // true se deleted_at
    protected $useTimestamps = true;
    protected $createdField  = 'created_at';
    protected $updatedField  = 'updated_at';
    protected $deletedField  = 'deleted_at';

    protected $allowedFields = [
        'id_outro',
        'nome',
    ];

    // Métodos customizados: somente sobre ESTA tabela
    public function existsByNomeAndOutro(string $nome, int $idOutro, ?int $idIgnorar = null): bool
    {
        $builder = $this->builder();
        $builder->where('nome', trim($nome));
        $builder->where('id_outro', $idOutro);
        if ($idIgnorar !== null) {
            $builder->where('id !=', $idIgnorar);
        }
        return $builder->countAllResults() > 0;
    }
}
```

**PROIBIDO no Model:** `join()`, joins implícitos ou dependência de outras tabelas. Isso vai no Repository.

### 4.3 DTOs (Create + Update)

**Arquivo:** `app/DTOs/[Modulo]/Feature/CreateFeatureDTO.php`

```php
<?php

namespace App\DTOs\e_Prefeitura\Feature;

final class CreateFeatureDTO
{
    public function __construct(
        public ?int $id_outro,
        public string $nome,
    ) {}

    public static function fromArray(array $data): self
    {
        $idOutro = $data['id_outro'] ?? null;
        $idOutro = ($idOutro === '' || $idOutro === null) ? null : (int) $idOutro;

        return new self(
            id_outro: $idOutro,
            nome:     trim((string) ($data['nome'] ?? '')),
        );
    }

    public function toArray(): array
    {
        return [
            'id_outro' => $this->id_outro,
            'nome'     => $this->nome,
        ];
    }
}
```

**Arquivo:** `app/DTOs/[Modulo]/Feature/UpdateFeatureDTO.php` — estrutura idêntica (pode reaproveitar via trait se for grande).

**Regras do DTO:**
- `final class` (não pode estender)
- Construtor com properties promovidas (PHP 8+)
- `fromArray()` sanitiza e converte (`trim`, cast para int, null para strings vazias)
- `toArray()` devolve array pronto pro model

### 4.4 Repository (SOMENTE se houver JOIN)

**Arquivo:** `app/Repositories/[Modulo]/FeatureRepository.php`

```php
<?php

namespace App\Repositories\e_Prefeitura;

use CodeIgniter\Database\BaseConnection;
use Config\Database;

class FeatureRepository
{
    private BaseConnection $db;

    public function __construct()
    {
        $this->db = Database::connect();
    }

    public function findAll(): array
    {
        return $this->db
            ->table('nome_tabela f')
            ->select('f.*, o.nome AS outro_nome')
            ->join('outra_tabela o', 'o.id = f.id_outro', 'left')
            ->orderBy('f.nome', 'ASC')
            ->get()
            ->getResult();
    }

    public function findOne(int $id): ?object
    {
        $row = $this->db
            ->table('nome_tabela f')
            ->select('f.*, o.nome AS outro_nome')
            ->join('outra_tabela o', 'o.id = f.id_outro', 'left')
            ->where('f.id', $id)
            ->get()
            ->getRow();

        return $row ?: null;
    }

    // Lookups auxiliares (para selects nos forms)
    public function findAllOutros(): array
    {
        return $this->db->table('outra_tabela')
            ->orderBy('nome', 'ASC')
            ->get()
            ->getResult();
    }
}
```

**Regras do Repository:**
- Usa `$this->db` direto (query builder)
- Alias curtos (`f`, `o`, `pr`) para tabelas
- Sempre trata `null` em `findOne`
- Pode conter lookup lists (`findAllCampi`, etc.)

### 4.5 Service (business logic + CRUD auditado)

**Arquivo:** `app/Services/[Modulo]/FeatureService.php`

```php
<?php

namespace App\Services\e_Prefeitura;

use App\DTOs\e_Prefeitura\Feature\CreateFeatureDTO;
use App\DTOs\e_Prefeitura\Feature\UpdateFeatureDTO;
use App\Models\e_Prefeitura\FeatureModel;
use App\Repositories\e_Prefeitura\FeatureRepository;   // se tiver JOIN
use App\Services\e_Prefeitura\Common\Audit\BaseAuditedCrudService;
use Config\Services;

class FeatureService extends BaseAuditedCrudService
{
    private FeatureModel $featureModel;
    private FeatureRepository $featureRepository; // opcional

    public function __construct()
    {
        $this->featureModel      = new FeatureModel();
        $this->featureRepository = new FeatureRepository(); // se tiver

        parent::__construct(
            model:    $this->featureModel,
            resource: 'nome_tabela' // nome da tabela para o audit
        );
    }

    // ── LEITURA ──
    public function findAllFeature(): array
    {
        return $this->featureRepository->findAll(); // COM repo
        // ou $this->featureModel->findAll();       // SEM repo
    }

    public function findOneFeature(int $id): ?object
    {
        return $this->featureRepository->findOne($id);
    }

    // ── CRUD ──
    public function createFeature(CreateFeatureDTO $dto): array
    {
        $data = $dto->toArray();
        $valid = $this->validateFeature($data);

        if (! $valid['ok']) {
            return [
                'success' => false,
                'message' => array_values($valid['errors'])[0] ?? 'Verifique os campos.',
                'errors'  => $valid['errors'],
            ];
        }

        try {
            $id = $this->auditedInsert($data, [
                'feature' => 'feature',
                'module'  => 'e_Prefeitura',
            ]);

            return $id
                ? ['success' => true,  'message' => 'Registro criado com sucesso.']
                : ['success' => false, 'message' => 'Não foi possível criar.'];
        } catch (\Throwable $e) {
            log_message('error', '[FeatureService::createFeature] {msg}', ['msg' => $e->getMessage()]);
            return ['success' => false, 'message' => 'Erro ao criar o registro.'];
        }
    }

    public function updateFeature(int $id, UpdateFeatureDTO $dto): array
    {
        $data       = $dto->toArray();
        $data['id'] = $id;
        $valid      = $this->validateFeature($data, $id);

        if (! $valid['ok']) {
            return [
                'success' => false,
                'message' => array_values($valid['errors'])[0] ?? 'Verifique os campos.',
                'errors'  => $valid['errors'],
            ];
        }

        try {
            $ok = $this->auditedUpdate($id, $data, [
                'feature' => 'feature',
                'module'  => 'e_Prefeitura',
            ]);
            return $ok
                ? ['success' => true,  'message' => 'Atualizado com sucesso.']
                : ['success' => false, 'message' => 'Não foi possível atualizar.'];
        } catch (\Throwable $e) {
            log_message('error', '[FeatureService::updateFeature] {msg}', ['msg' => $e->getMessage()]);
            return ['success' => false, 'message' => 'Erro ao atualizar.'];
        }
    }

    public function deleteFeature(int $id): array
    {
        try {
            $ok = $this->auditedDelete($id, false, [
                'feature' => 'feature',
                'module'  => 'e_Prefeitura',
                'purge'   => false, // true = hard delete
            ]);
            return $ok
                ? ['success' => true,  'message' => 'Excluído com sucesso.']
                : ['success' => false, 'message' => 'Não foi possível excluir.'];
        } catch (\Throwable $e) {
            log_message('error', '[FeatureService::deleteFeature] {msg}', ['msg' => $e->getMessage()]);
            return ['success' => false, 'message' => 'Erro ao excluir.'];
        }
    }

    // ── VALIDAÇÃO ──
    public function validateFeature(array $data, ?int $id = null): array
    {
        $validation = Services::validation();

        $rules = [
            'nome'     => 'required|min_length[2]|max_length[255]',
            'id_outro' => 'permit_empty|integer|is_not_unique[outra_tabela.id]',
        ];

        $errors = [
            'nome' => [
                'required'   => 'O campo nome é obrigatório.',
                'min_length' => 'Mínimo 2 caracteres.',
                'max_length' => 'Máximo 255 caracteres.',
            ],
            'id_outro' => [
                'integer'       => 'Campo inválido.',
                'is_not_unique' => 'Referência não existe.',
            ],
        ];

        $validation->setRules($rules, $errors);

        if (! $validation->run($data)) {
            return ['ok' => false, 'errors' => $validation->getErrors()];
        }

        // Regras de negócio (ex.: unicidade)
        $nome     = trim((string) ($data['nome'] ?? ''));
        $idOutro  = (int) ($data['id_outro'] ?? 0);
        if ($idOutro > 0 && $this->featureModel->existsByNomeAndOutro($nome, $idOutro, $id)) {
            return ['ok' => false, 'errors' => ['nome' => 'Já existe registro com este nome.']];
        }

        return ['ok' => true];
    }
}
```

### 4.6 BaseAuditedCrudService — métodos herdados

`app/Services/e_Prefeitura/Common/Audit/BaseAuditedCrudService.php`:

| Método | Retorno | Uso |
|---|---|---|
| `auditedInsert(array $data, array $meta)` | `int\|false` (ID inserido) | Persiste com log de auditoria |
| `auditedUpdate(int $id, array $data, array $meta)` | `bool` | Update com diff antes/depois |
| `auditedDelete(int $id, bool $purge, array $meta)` | `bool` | Soft delete (default) ou hard (`purge=true`) |
| `actorUserId(): ?int` | `?int` | ID do usuário logado (da sessão) |
| `fetchRow(int $id, bool $withDeleted)` | `array` | Busca 1 linha normalizada em array |
| `normalizeRow(mixed $row)` | `array` | Converte Entity/object/array → array |

**⚠️ Em CLI** a sessão não existe; `actorUserId()` retorna `null`. O insert continua funcionando, só o log de audit que pode falhar.

### 4.7 Controller (thin)

**Arquivo:** `app/Controllers/[Modulo]/Admin/FeatureController.php`

```php
<?php

namespace App\Controllers\e_Prefeitura\Admin;

use App\Controllers\BaseController;
use App\DTOs\e_Prefeitura\Feature\CreateFeatureDTO;
use App\DTOs\e_Prefeitura\Feature\UpdateFeatureDTO;
use App\Services\e_Prefeitura\FeatureService;

class FeatureController extends BaseController
{
    private FeatureService $service;
    private const ROUTE_BASE = 'admin/feature';

    public function __construct()
    {
        $this->service = new FeatureService();
        helper(['url', 'form']);
    }

    public function index()
    {
        return $this->render('e_Prefeitura/admin/feature/index', [
            'items' => $this->service->findAllFeature(),
        ]);
    }

    public function criar()
    {
        return $this->render('e_Prefeitura/admin/feature/form', [
            'item' => null,
            // dados auxiliares: $this->service->findAllOutros() etc.
        ]);
    }

    public function store()
    {
        $dto    = CreateFeatureDTO::fromArray($this->request->getPost());
        $result = $this->service->createFeature($dto);

        if ($result['success']) {
            return redirect()->to(base_url(self::ROUTE_BASE))
                             ->with('success', $result['message']);
        }
        return redirect()->back()->withInput()->with('error', $result['message']);
    }

    public function editar(int $id)
    {
        $item = $this->service->findOneFeature($id);
        if (! $item) {
            return redirect()->to(base_url(self::ROUTE_BASE))->with('error', 'Não encontrado.');
        }
        return $this->render('e_Prefeitura/admin/feature/form', ['item' => $item]);
    }

    public function update(int $id)
    {
        $dto    = UpdateFeatureDTO::fromArray($this->request->getPost());
        $result = $this->service->updateFeature($id, $dto);

        if ($result['success']) {
            return redirect()->to(base_url(self::ROUTE_BASE))->with('success', $result['message']);
        }
        return redirect()->back()->withInput()->with('error', $result['message']);
    }

    public function delete(int $id)
    {
        $result = $this->service->deleteFeature($id);
        return redirect()->to(base_url(self::ROUTE_BASE))
                         ->with($result['success'] ? 'success' : 'error', $result['message']);
    }
}
```

**Regras do Controller:**
- **Thin** — nunca lógica de negócio aqui
- Apenas: `request → DTO → Service → response`
- Se precisar de dados auxiliares (listas pra selects), pega do Service (`findAllOutros()`)

### 4.8 Rotas

**Arquivo:** `app/Config/Routes.php`

```php
$routes->group('admin', ['filter' => 'meuFiltroAuth'], function ($routes) {
    // Feature
    $routes->get('feature',                   'e_Prefeitura\Admin\FeatureController::index');
    $routes->get('feature/criar',             'e_Prefeitura\Admin\FeatureController::criar');
    $routes->post('feature/store',            'e_Prefeitura\Admin\FeatureController::store');
    $routes->get('feature/editar/(:num)',     'e_Prefeitura\Admin\FeatureController::editar/$1');
    $routes->post('feature/update/(:num)',    'e_Prefeitura\Admin\FeatureController::update/$1');
    $routes->get('feature/delete/(:num)',     'e_Prefeitura\Admin\FeatureController::delete/$1');
});
```

### 4.9 Views

**Engine:** PHP nativo do CI4 com template inheritance.

**Arquivo:** `app/Views/[Modulo]/admin/feature/index.php`

```php
<?= $this->extend('e_Prefeitura/template/base'); ?>
<?= $this->section('content'); ?>

<h2 class="mb-4 text-center">Feature</h2>

<?php if (session()->getFlashdata('error')): ?>
    <div class="alert alert-danger"><?= session()->getFlashdata('error') ?></div>
<?php endif; ?>
<?php if (session()->getFlashdata('success')): ?>
    <div class="alert alert-success"><?= session()->getFlashdata('success') ?></div>
<?php endif; ?>

<a href="<?= base_url('admin/feature/criar') ?>" class="btn btn-primary">Novo</a>

<table id="tabela" class="display" style="width:100%;">
    <thead>
        <tr><th>ID</th><th>Nome</th><th>Ações</th></tr>
    </thead>
    <tbody>
        <?php // Vazio? Deixe o <tbody> SEM <tr> — a DataTables mostra o "emptyTable" do pt-BR.json.
              // NUNCA use <tr><td colspan>…</td></tr> aqui: quebra a init da DataTable (ver nota abaixo). ?>
        <?php foreach (($items ?? []) as $item): ?>
            <tr>
                <td><?= esc($item->id) ?></td>
                <td><?= esc($item->nome) ?></td>
                <td>
                    <a href="<?= base_url("admin/feature/editar/{$item->id}") ?>" class="btn btn-sm btn-primary">Editar</a>
                    <a href="<?= base_url("admin/feature/delete/{$item->id}") ?>"
                       class="btn btn-sm btn-danger"
                       onclick="return confirm('Excluir?');">Excluir</a>
                </td>
            </tr>
        <?php endforeach; ?>
    </tbody>
</table>

<script>
$(function() {
    $('#tabela').DataTable({
        language: { url: '<?= base_url('public/assets/e_Prefeitura/vendor/datatables/pt-BR.json'); ?>' },
        responsive: true,
    });
});
</script>

<?= $this->endSection(); ?>
```

**Conceitos-chave:**
- `$this->extend('Modulo/template/base')` → layout herdado
- `$this->section('content') ... $this->endSection()` → blocos de conteúdo
- `esc()` obrigatório em qualquer output de dados (anti-XSS)
- `csrf_field()` obrigatório em forms POST
- `base_url('rota')` para construir URLs

**⚠️ Estado vazio — NÃO renderize linha-placeholder com `colspan` dentro da DataTable.**
Quando a lista vier vazia, deixe o `<tbody>` **sem nenhum `<tr>`**: a própria DataTables mostra
a mensagem `emptyTable` do `pt-BR.json` ("Nenhum registro encontrado"). **Nunca** faça isto:

```php
<tbody>
    <?php if (empty($items)): ?>
        <tr><td colspan="3">Nenhum registro.</td></tr>   <!-- ❌ QUEBRA a init da DataTable -->
    <?php else: ?>
        <?php foreach ($items as $item): ?> ... <?php endforeach; ?>
    <?php endif; ?>
</tbody>
```

O `<thead>` tem N colunas, mas a linha-placeholder tem **1 célula** (`colspan`). A DataTables
tenta ler as colunas 2…N dessa linha, não acha, dispara *"Requested unknown parameter"* e
**aborta a inicialização** — o que derruba qualquer `initComplete` (o botão de ação da §4.9.1
**some** justo quando a lista está vazia) e polui o console com o warning. `<tbody>` vazio → a
init conclui e a DataTables renderiza o `emptyTable` sozinha. Para trocar o texto de vazio,
defina um `emptyTable` próprio — mas note que ele **não combina** com `language.url` (o `url`
substitui o objeto de idioma inteiro).

### 4.9.1 Botão de ação na barra da DataTable (reutilizável)

Para colocar um botão (ex.: **"Novo"**) **dentro da barra da tabela**, ao lado do seletor
"Exibir N registros", use o callback `initComplete` da DataTable e injete o botão no
container `#<id>_length` (gerado pela própria DataTables). Derivando o id da tabela pela
**API** (`this.api().table().node().id`), o trecho fica **genérico** — copiar/colar em
qualquer `index.php`, sem hardcode do id:

```php
<table id="tabela" class="display" style="width:100%;"> ... </table>

<script>
$(function () {
    $('#tabela').DataTable({
        language: { url: '<?= base_url('public/assets/e_Prefeitura/vendor/datatables/pt-BR.json'); ?>' },
        responsive: true,
        lengthMenu: [[10, 25, 50, -1], [10, 25, 50, 'Todos']],

        // Injeta um botão de ação na barra da tabela (ao lado de "Exibir N registros").
        initComplete: function () {
            const idLength = '#' + this.api().table().node().id + '_length';

            const btn = `
                <a href="<?= base_url('admin/feature/criar') ?>" class="btn btn-primary btn-sm me-3">
                    <i class="fa fa-plus"></i> Novo
                </a>`;

            $(idLength).addClass('d-flex align-items-center').prepend(btn);
            $(idLength + ' label').addClass('mb-0'); // alinha o botão com o texto nativo
        },
    });
});
</script>
```

**Como funciona:**
- A DataTables cria um container `#<id-da-tabela>_length` com o "Exibir N registros".
  `this.api().table().node().id` devolve o id da `<table>`, então `#<id>_length` serve para
  **qualquer** tabela (evita o hardcode `#grupos_length`, `#espacos_length`, …).
- `.prepend(btn)` põe o botão **antes** do seletor; `.addClass('d-flex align-items-center')`
  alinha os dois na mesma linha; `label.mb-0` remove a margem nativa do rótulo.
- **Vários botões:** injete um wrapper (`<div class="d-flex gap-2 me-3">…</div>`) no lugar do
  `<a>` único. Para botão à **direita** (na área de busca), troque o alvo por `#<id>_filter`.
- Sempre `esc()`/escape em conteúdo dinâmico dentro do HTML injetado (anti-XSS).
- **⚠️ Estado vazio:** o `initComplete` só dispara se a init **concluir**. Uma linha-placeholder
  `<tr><td colspan="N">…</td></tr>` no `<tbody>` aborta a init e **o botão some quando a lista
  fica vazia** — deixe o `<tbody>` sem `<tr>` (a DataTables mostra o `emptyTable`). Ver a nota da §4.9.

**Alternativa (extensão *Buttons*):** se a tela já usa a extensão *DataTables Buttons*, dá
para declarar `layout: { topStart: 'buttons' }` + `buttons: [...]`. O `initComplete` acima
dispensa a dependência extra e basta para um botão de "Novo".

> **Exemplo real no projeto:** `app/Views/e_EspacoFisico/admin/grupos_recursos/index.php`
> (botão "Novo Grupo" injetado no `#grupos_length`).

### 4.10 Instanciação de dependências (nunca `new` no meio do método)

**Regra:** toda dependência (Service, Model, Repository) é declarada como **propriedade
tipada** e instanciada **uma única vez**, no topo da classe. Importe com `use` —
**nunca** FQN inline no corpo do método.

❌ **Evite:**

```php
public function index()
{
    // ...
    $termoService         = new \App\Services\e_Transporte\TermoService();   // ❌
    $precisaAceitarTermos = ! $termoService->usuarioAceitouVersaoAtual($userId);
    $termoVersao          = $termoService->versaoAtual();
}
```

Por que é ruim: se a classe for usada em **outro método**, o `new` se **repete**; a
dependência fica **escondida** no meio da lógica (não dá para saber o que a classe usa
olhando o topo); e o FQN gigante polui o corpo do método.

✅ **Faça:**

```php
use App\Services\e_Transporte\TermoService;

class SolicitacoesController extends BaseController
{
    private TermoService $termoService;

    public function initController(RequestInterface $request, ResponseInterface $response, LoggerInterface $logger)
    {
        parent::initController($request, $response, $logger);
        $this->termoService = new TermoService();
    }

    public function index()
    {
        $precisaAceitarTermos = ! $this->termoService->usuarioAceitouVersaoAtual($userId);
        $termoVersao          = $this->termoService->versaoAtual();
    }
}
```

**Onde instanciar:**

| Camada | Onde |
|---|---|
| Controller | `initController()` — obrigatório se o setup precisar de `$request`/`$response`; `__construct()` serve quando não precisa |
| Service | `__construct()` |
| Repository | `__construct()` (`Database::connect()`) |

**Única exceção tolerável:** classe usada em **um só ponto**, barata de construir e fora do
caminho quente (ex.: um gerador de PDF chamado em um único método). Mesmo assim, importe
com `use` em vez de FQN inline.

### 4.11 Diálogos e alertas — SweetAlert2 (nunca `alert()`/`confirm()` nativos)

**Regra:** toda confirmação ("Excluir?", "Encerrar turno?") e todo aviso ao usuário usam o
**SweetAlert2 que já está no projeto**. **Proibido**:

- `confirm()`, `alert()`, `prompt()` **nativos** do JavaScript (feios, não estilizáveis,
  travam a UI e destoam do resto);
- **baixar o SweetAlert de novo** (CDN, `npm`, outra cópia). Já existe **uma** cópia versionada:

```
public/assets/e_EspacoFisico/vendor/sweet-alert/sweetalert2@11.js
```

> O caminho é sob `e_EspacoFisico` por ser onde entrou primeiro, mas a lib é **global** —
> use essa mesma para qualquer módulo. Não duplique o arquivo por módulo.

**Carregar** (no fim da view, antes do seu `<script>`):

```php
<script src="<?= base_url('public/assets/e_EspacoFisico/vendor/sweet-alert/sweetalert2@11.js'); ?>"></script>
```

**❌ Evite** (o que ainda existe espalhado nas telas admin legadas — não replique):

```php
<a href="<?= base_url("admin/x/delete/{$id}") ?>"
   onclick="return confirm('Tem certeza?');">Excluir</a>   <!-- ❌ confirm nativo -->
```

**✅ Faça — padrão `[data-confirm]`** (declarativo, delegado, sem `onclick`): marque o
link/botão/form com `data-confirm` (e `data-confirm-title` opcional) e deixe **um** listener
global tratar todos. É o padrão já implementado em `app/Views/e_Transporte/bdt/folha.php`
(copie de lá) — funciona em `<a>`, `<button type="submit">` e `<form>`, preserva o
`name/value` do botão acionado e cai no `confirm()` nativo **só** como último recurso se o
Swal não tiver carregado:

```php
<!-- Link -->
<a href="<?= base_url("transporte/admin/x/delete/{$id}") ?>"
   data-confirm="Excluir este registro?" data-confirm-title="Excluir?">
    Excluir
</a>

<!-- Form (POST) -->
<form method="post" action="<?= base_url('...') ?>" data-confirm="Encerrar o turno deste condutor?">
    <?= csrf_field(); ?>
    <button type="submit" class="btn btn-sm btn-outline-secondary">Encerrar turno</button>
</form>
```

```js
// Listener único — recorte do folha.php. `confirmar()` devolve Promise<bool>.
function confirmar(msg, titulo) {
    if (typeof Swal === 'undefined') { return Promise.resolve(window.confirm(msg)); } // fallback
    return Swal.fire({
        title: titulo || 'Confirmar?', text: msg, icon: 'question',
        showCancelButton: true, confirmButtonText: 'Sim', cancelButtonText: 'Cancelar',
        confirmButtonColor: '#0072CE',
    }).then(r => r.isConfirmed);
}
// delega em [data-confirm] para links, botões e forms — ver folha.php para o corpo completo.
```

**Feedback (sucesso/erro)** — para mensagem simples pós-ação, prefira o **flashdata**
(`with('success', ...)` → o `alert` Bootstrap que as views já renderizam). SweetAlert como
**toast** só quando fizer sentido (ação via AJAX, sem reload):

```js
Swal.fire({ toast: true, position: 'top-end', timer: 2500, showConfirmButton: false,
            icon: 'success', title: 'Salvo.' });
```

### 4.12 Campos obrigatórios — validação inline (nunca o `required` nativo)

**Regra:** campo obrigatório é validado por **JavaScript com erro inline** (erro bonito,
em português, ancorado no campo), **não** pelo `required` do HTML — que mostra um balão feio
e não estilizável, e **nem dispara** quando o Salvar é `type="button"` (o submit é feito por
JS depois de validar).

> ⚠️ **Isto é UX, NÃO segurança.** Validação no cliente — `required` nativo **ou** este JS —
> é sempre burlável (DevTools, `curl`, JS desligado). A **trava real é server-side**: o
> Service/Model valida no insert (§4.5, `validate()` / `validationRules`) e o banco tem os
> `NOT NULL`/FKs. **Nunca** confie só no JS para integridade. O JS só evita o ida-e-volta.

**Telas NOVAS: use o validador genérico compartilhado** — nada de reescrever validação por
tela. É **declarativo**, dirigido por atributo:

```
public/assets/e_Prefeitura/js/form-validate.js
```

```php
<!-- 1) marca o form + os campos; o slot de erro é criado sozinho se faltar -->
<form action="<?= base_url('...') ?>" method="post" data-validate>
    <?= csrf_field() ?>

    <label for="justificativa" class="form-label">Justificativa *</label>          <!-- ' *' visual -->
    <textarea name="justificativa" id="justificativa" class="form-control"
              data-required data-label="Justificativa"></textarea>                 <!-- SEM required nativo -->
    <div id="divError-justificativa" class="invalid-feedback"></div>               <!-- opcional; auto-criado -->

    <button type="submit" class="btn btn-primary">Salvar</button>                  <!-- barrado se inválido -->
</form>

<script src="<?= base_url('public/assets/e_Prefeitura/js/form-validate.js'); ?>"></script>
```

- Salvar como **`type="submit"`** → o `form-validate.js` barra o envio se inválido.
- Salvar como **`type="button"`** (padrão das telas com JS) → marque-o com
  `data-validate-submit` (valida e, se ok, envia).
- Mensagem: `data-required-msg="..."`, senão `data-label`, senão o texto do `<label for>`.
- Bloco condicional oculto (ex.: "carga"): deixe os inputs `disabled` — desabilitado é ignorado.
- Validações extras (regra de negócio) antes de enviar: `window.FormValidate.validate(form)` +
  `showError(el, msg)` / `clearError(el)`.

❌ **Não** use `<input required>` como a trava de obrigatoriedade.

> **Legado (não replicar para casos simples):** o form de **Solicitações**
> (`requests/index.php` + `…/js/requests/validate.js`) tem validação própria, acoplada, com
> regras de negócio (trechos, datas, passageiros). Fica **como está**; telas novas usam o
> `form-validate.js` acima.

### 4.13 Upload de imagens — preview do novo + galeria das já salvas

Duas áreas **separadas e distintas** (nunca misturadas):

**(A) Preview do que está sendo adicionado** — antes de enviar. `<input type="file" multiple>`
+ um `<div id="preview…">`; um JS (`FileReader`) monta os thumbs das imagens **recém-escolhidas**,
cada uma com um "×" que só a **tira da fila** (não mexe em nada salvo). Referência:
`…/js/manutencao/script.js` (`renderPreview()`).

**(B) Imagens já cadastradas** — em bloco **próprio**, abaixo/ao lado do preview, com um rótulo
"Fotos já cadastradas". Cada uma:
- é um thumb que abre o **modal + carrossel** — padrão `img[data-group="…"]` das ocorrências
  da `folha.php` (o mesmo `galeriaModal` genérico; ver §4.11 de reuso de assets, e a galeria
  em `app/Views/e_Transporte/bdt/folha.php`);
- tem o checkbox **"Remover" EMBAIXO** da imagem — **nunca** sobreposto em cima. Checkbox em
  cima da imagem vira armadilha: clicar na foto (instinto de ampliar) marca a remoção e ela
  some ao salvar. O remove viaja como `name="remove_fotos_{grupo}[]"`.

```php
<!-- (A) preview do novo -->
<input type="file" name="fotos_antes[]" id="fotos_antes" class="form-control" accept="image/*" multiple>
<div id="previewAntes" class="d-flex flex-wrap gap-3 mt-3"></div>

<!-- (B) já salvas — SEPARADO, com carrossel e o checkbox EMBAIXO -->
<?php if (! empty($anexosAntes)): ?>
    <hr>
    <label class="form-label fw-semibold text-secondary">Fotos já cadastradas:</label>
    <div class="d-flex flex-wrap gap-3">
        <?php foreach ($anexosAntes as $foto): ?>
            <div class="text-center">
                <img src="<?= esc(base_url('documentos/view/' . (int) $foto->id)) ?>"
                     data-group="antes" alt="Foto" title="Clique para ampliar"
                     class="rounded border" style="width:110px;height:110px;object-fit:cover;cursor:pointer;">
                <div class="form-check d-flex justify-content-center gap-1 mt-1">
                    <input type="checkbox" name="remove_fotos_antes[]" value="<?= (int) $foto->id ?>"
                           class="form-check-input" id="rmAntes<?= (int) $foto->id ?>">
                    <label class="form-check-label small text-danger" for="rmAntes<?= (int) $foto->id ?>">Remover</label>
                </div>
            </div>
        <?php endforeach; ?>
    </div>
<?php endif; ?>
```

> O `manutencoes/form.php` é a referência de **preview** (`renderPreview()` em
> `manutencao/script.js`) e já está **alinhado** a esta seção: checkbox "Remover" **embaixo**,
> thumbs abrindo o modal+carrossel (`img[data-group]`), `data-required` (§4.12) e SweetAlert
> (§4.11) no lugar de `required`/`alert()`. A galeria canônica (carrossel) é a das
> **ocorrências** na `folha.php`.

### 4.14 Botão que é link de navegação — `<button>` dentro do `<a>`

**Regra:** quando um **link** precisa ter cara de botão (navegação: "Voltar", "Ir para o
Dashboard", "Exportar PDF"), ponha a classe `.btn` num **`<button type="button">` interno** —
não no próprio `<a>`. O `<a>` fica só com o posicionamento (`d-inline-flex`, `gap-1`); o
`<button>` carrega o visual (`btn btn-outline-secondary`, etc.).

**Por quê:** o `style_system.css` tem um override global com `!important` na cor de link:

```css
a { color: #39769f !important; }
```

Como é `!important` **e** mira o seletor `a`, ele **vence** as regras de cor do Bootstrap quando
o `.btn` está no próprio `<a>`: o texto do botão fica preso no azul `#39769f` inclusive **no
hover** (onde o `.btn-*:hover` deveria deixá-lo branco). Resultado: no hover o fundo muda mas o
texto continua azulado → **contraste ruim / lavado**. Com o `.btn` num `<button>` interno, a
regra `a { color … }` não alcança o texto do botão (o `<button>` traz a própria cor do `.btn`),
e o `:hover` do Bootstrap volta a funcionar (texto branco).

❌ **Evite** — `.btn` direto no `<a>` (hover lavado pelo `a { color:…!important }`):

```php
<a href="<?= base_url('transporte/admin/relatorios') ?>"
   class="btn btn-outline-secondary d-inline-flex align-items-center gap-1">
    <i class="bi bi-arrow-left"></i> Voltar às entidades
</a>
```

✅ **Faça** — `.btn` num `<button type="button">`; o `<a>` só posiciona:

```php
<a href="<?= base_url('transporte/admin/relatorios') ?>"
   class="d-inline-flex align-items-center gap-1">
    <button type="button" class="btn btn-outline-secondary">
        <i class="bi bi-arrow-left"></i> Voltar às entidades
    </button>
</a>
```

**Detalhes:**
- **`type="button"` é obrigatório** no botão interno. Sem ele o default é `submit`; se o link
  cair dentro de um `<form>`, o clique enviaria o form. Com `type="button"` o botão é inerte e
  **quem navega é o `<a>`** (o clique borbulha até ele).
- Vale **só para link com cara de botão** (navegação). Botão de ação real (submit de form, abrir
  modal, disparar JS) continua sendo um `<button class="btn">`/`.btn` normal — **não** embrulhe
  esses num `<a>`.
- Classes de posição (`d-inline-flex`, `align-items-center`, `gap-1`) ficam no `<a>`; só
  ícone+texto vão no `<button>`.

> **Ressalva honesta:** `<button>` dentro de `<a>` não é HTML 100% válido (o validador acusa
> "interactive content"), mas funciona em todos os navegadores e o `type="button"` mantém o
> clique previsível. A alternativa "purista" seria outro override de CSS escopado
> (`a.btn:hover { color:#fff !important }`) — empilhar `!important` sobre `!important`; o embrulho
> é mais simples e é o **padrão adotado**.
> **Referência real:** `app/Views/e_Transporte/admin/relatorios/entidade.php` ("Voltar às
> entidades", "Exportar PDF").

---

## 5. Convenções de Nomenclatura

| Camada | Caminho | Namespace |
|---|---|---|
| Controller | `app/Controllers/[Modulo]/Admin/FeatureController.php` | `App\Controllers\[Modulo]\Admin\FeatureController` |
| Service | `app/Services/[Modulo]/FeatureService.php` | `App\Services\[Modulo]\FeatureService` |
| Repository | `app/Repositories/[Modulo]/FeatureRepository.php` | `App\Repositories\[Modulo]\FeatureRepository` |
| Model | `app/Models/[Modulo]/FeatureModel.php` | `App\Models\[Modulo]\FeatureModel` |
| DTO Create | `app/DTOs/[Modulo]/Feature/CreateFeatureDTO.php` | `App\DTOs\[Modulo]\Feature\CreateFeatureDTO` |
| DTO Update | `app/DTOs/[Modulo]/Feature/UpdateFeatureDTO.php` | `App\DTOs\[Modulo]\Feature\UpdateFeatureDTO` |
| Views | `app/Views/[Modulo]/admin/feature/{index,form}.php` | — |
| Migration | `app/Database/Migrations/YYYY-MM-DD-HHMMSS_ClassName.php` | `App\Database\Migrations\ClassName` |

**Nomes de métodos por camada:**
- **Service:** `findAllFeature`, `findOneFeature`, `createFeature`, `updateFeature`, `deleteFeature`, `validateFeature`
- **Repository:** `findAll`, `findOne`, `find[Lookup]List` (ex.: `findAllCampi`)
- **Model:** `existsByXxx`, `findByXxx` (custom queries em 1 tabela)

---

## 6. Filtros (Auth)

**Arquivo:** `app/Filters/[Modulo]AuthFilter.php`

Cada módulo tem seu próprio filtro. Registra em `app/Config/Filters.php`:

```php
public array $aliases = [
    'meuFiltroAuth' => \App\Filters\MeuFiltroAuth::class,
];
```

Aplicado em rotas:

```php
$routes->group('admin', ['filter' => 'meuFiltroAuth'], function ($routes) { ... });
```

O filtro:
- Redireciona para login se não autenticado
- Valida papel/role do usuário
- Popula contexto compartilhado (via sessão)

**Exemplo real:** `app/Filters/EspacoFisicoAuthFilter.php` mapeia rotas admin → papéis necessários:

```php
$map = [
    'agendamento/regras-espacos' => 'regrasDeAgendamento',
    'agendamento/regras'         => 'regrasDeAgendamento',
    'eventos'                    => 'cadastroAdmManual',
    'espacos'                    => 'cadastroDeEspacos',
    'recursos'                   => 'cadastroDeRecursos',
];
```

### 6.1 Papéis (roles) — sempre por constante

**Nunca** use a string do papel solta. Cada módulo tem sua classe de constantes
(ex.: `app/Constants/TransporteRoles.php`) — ela é a **fonte única**:

```php
// ❌ string mágica: quebra silenciosamente se o nome mudar no banco
$this->usuariosPapeisModel->usuarioTemPapel($userId, 'Cadastro de Tipos');

// ✅ constante
use App\Constants\TransporteRoles;
$this->usuariosPapeisModel->usuarioTemPapel($userId, TransporteRoles::CADASTRO_TIPOS);
```

**O flag do filtro vem do NOME da constante** (camelCase), não do valor:

| Constante | Valor (no banco) | Flag no `$map` / `$can()` |
|---|---|---|
| `CADASTRO_TIPOS` | `'Cadastro de Tipos'` | `cadastroTipos` |
| `CRIAR_BDT_SEM_SOLICITACAO` | `'Criar BDT sem Solicitação'` | `criarBdtSemSolicitacao` |

**Admin do módulo passa em tudo:** o filtro faz `if ($isAdminFull) return;` **antes** de
checar o `$map`. Então mapear uma rota a um papel significa, na prática,
**"admin do módulo OU quem tem o papel"** — use isso a favor em vez de criar papel novo.

**Criar papel novo** — só quando **nenhum existente** couber (evite proliferar papéis):

1. Constante em `[Modulo]Roles.php`
2. Migration idempotente: insere em `papeis` + atribui aos admins
3. Entrada no `PapeisSeeder`
4. Entrada no `$map` do filtro (rota → flag)
5. `$can('flag')` no dashboard, para exibir o card

> **Reuso antes de criar:** o catálogo de tipos de ocorrência (W15) reusou `CADASTRO_TIPOS`
> por ser a mesma família de catálogos auxiliares; o histórico de ocorrências reusou
> `CRIAR_BDT_SEM_SOLICITACAO`. Nenhum papel novo foi criado.

---

## 7. Checklist para Criar uma Nova Feature

Use esta sequência ao criar uma feature nova:

1. [ ] **Migration** — criar tabela (`docker exec apache php spark make:migration Xxx`)
2. [ ] **Rodar migration** — `docker exec apache php spark migrate`
3. [ ] **Model** — uma tabela, `allowedFields`, métodos `existsByXxx`
4. [ ] **Repository** — *somente se tiver JOIN* — queries com `$this->db->table('...')->join(...)`
5. [ ] **DTOs** — `CreateXxxDTO` + `UpdateXxxDTO` com `fromArray()` e `toArray()`
6. [ ] **Service** — estende `BaseAuditedCrudService`, métodos `findAll/findOne/create/update/delete/validate`
7. [ ] **Controller** — thin, apenas recebe request → DTO → Service → render/redirect
8. [ ] **Rotas** — adicionar em `app/Config/Routes.php` sob grupo com filtro
9. [ ] **Filtro** — mapear rota → papel necessário (se aplicável)
10. [ ] **Views** — `index.php` (lista DataTable) + `form.php` (criar/editar)
11. [ ] **Dashboard link** — adicionar botão em `app/Views/[Modulo]/admin/dashboard.php`
12. [ ] **Auditoria** — toda escrita sensível vai pro log (`BaseAuditedCrudService` ou `AuditLogger` direto). Ver §8.
13. [ ] **Dependências** — Service/Model/Repository como **propriedade tipada**, instanciados no `initController()`/`__construct()`. Nunca `new` no meio do método. Ver §4.10.
14. [ ] **Papéis** — reusar papel existente antes de criar um novo; sempre via constante `[Modulo]Roles::X`. Ver §6.1.
15. [ ] **Anexos** — arquivo sempre pelo `DocumentoService`; tipo sempre como constante no `DocTiposModel` + migration idempotente. Ver §9.
16. [ ] **Diálogos** — confirmação/aviso pelo **SweetAlert2 já no projeto** (`[data-confirm]`); nunca `alert()`/`confirm()` nativos nem CDN. Ver §4.11.
17. [ ] **Obrigatórios** — marcar campos com `data-required` + carregar o `form-validate.js` compartilhado (nunca o `required` nativo); e **validar server-side** de verdade (§4.5), pois o JS é só UX. Ver §4.12.
18. [ ] **Imagens** — preview do novo separado das já salvas; salvas em modal+carrossel (`data-group`), checkbox "Remover" **embaixo** da imagem. Ver §4.13.
19. [ ] **Testar** — syntax (`php -l`), rotas (curl HTTP), integração (command spark custom)
20. [ ] **Botão-link** — link com cara de botão (navegação: "Voltar", "Exportar PDF") = `.btn` num `<button type="button">` interno, nunca no próprio `<a>` (hover do CSS global). Ver §4.14.

### Decisão rápida: preciso de Repository?

| Cenário | Repository? |
|---|---|
| CRUD de 1 tabela, sem lookups | ❌ Não — Model basta |
| Listagem precisa do nome do campus/prédio/pai? | ✅ Sim |
| Queries com `MIN/MAX/GROUP BY` entre tabelas | ✅ Sim |
| Apenas validação com `is_not_unique[outra.id]` | ❌ Não — Service valida, Model insere |
| Dashboard com agregados de várias tabelas | ✅ Sim (pode ser na própria do dashboard) |

---

## 8. Auditoria (trilha de alterações)

> **Toda escrita sensível deve deixar rastro.** A auditoria responde: **quem** fez,
> **o quê** mudou (antes → depois), **quando**, por qual **rota/método** e de qual **IP**.

### Onde ver

Tela admin: **`admin/logs/auditoria`** (view `app/Views/e_Prefeitura/admin/audit_logs_index.php`).
É uma DataTable *server-side* com filtros (ação, recurso, método, usuário, data) e um
modal de detalhes com abas **Mudanças (diff) / Antes / Depois / Meta**. Os dados ficam na
tabela `audit_logs` (model `App\Models\e_Prefeitura\AuditLogModel`).

### Como gravar — duas formas

**(A) CRUD padrão → automático.** Se o Service estende `BaseAuditedCrudService`
(`app/Services/e_Prefeitura/Common/Audit/BaseAuditedCrudService.php`) e persiste via
`auditedInsert` / `auditedUpdate` / `auditedDelete`, o log sai sozinho (CREATE/UPDATE/DELETE
com `before`/`after` capturados da própria tabela). É o caminho do template da §4.5–4.6.

**(B) Ação fora do CRUD padrão → `AuditLogger` direto.** Para key/value, toggles, login,
ou qualquer ação que não passe pelo `auditedX`, injete o
`App\Services\e_Prefeitura\Common\Audit\AuditLogger` e chame `log(...)`:

```php
use App\Services\e_Prefeitura\Common\Audit\AuditLogger;

$this->audit = $audit ?? new AuditLogger();

$this->audit->log(
    action:      'UPDATE',                 // CREATE | UPDATE | DELETE (ou ação custom)
    resource:    'trnsp_configuracoes',    // nome lógico/tabela do recurso
    resourceId:  $row->id,
    actorUserId: $this->actorUserId(),     // session 'usuario_id' (null em CLI)
    before:      ['valor' => $antes],
    after:       ['valor' => $novo],
    meta:        ['chave' => $row->chave, 'grupo' => $row->grupo, 'feature' => 'transporte_configuracoes'],
);
```

`actorUserId()` = `session()->get('usuario_id') ?? session()->get('user_id')` (o
`BaseAuditedCrudService` já expõe esse método; em services soltos, replique-o).

### Garantias do AuditLogger

- **Mascara** `cpf`, `email`, `telefone`, `celular`; **redige** (`[REDACTED]`) `senha`, `password`, `token`, `*_token`.
- **Captura sozinho** `route`, `method`, `ip`, `user_agent`, `request_id` e `created_at` — não passe isso no payload.
- **Monta o diff** comparando `before` × `after` (só as chaves que mudaram entram em `changes_json`).
- **Nunca derruba o fluxo:** qualquer erro de auditoria é só logado (`AUDIT_LOG_FAIL`), a operação principal continua.

> ⚠️ Correções feitas **direto no banco** (phpMyAdmin) **não passam** pelo AuditLogger.
> Quando inevitável (ex.: campo que a app não edita), registre manualmente o motivo/data
> (LGPD Art. 37) — a app só audita o que passa por ela.

**Referência real:** `app/Services/e_Transporte/ConfiguracaoService.php::set()` (forma B) e
`BaseAuditedCrudService` (forma A).

---

## 9. Documentos e anexos (arquivos)

> **Nunca** grave arquivo na mão nem crie tabela própria de anexo. Todo upload passa pelo
> **`DocumentoService`**, e todo **tipo** de documento é uma **constante** no `DocTiposModel`.

### 9.1 Tipos de documento — fonte única

Os tipos vivem **somente** em `app/Models/e_Documentos/DocTiposModel.php`, como constantes:

```php
public const CNH                    = 'CNH';
public const CRLV                   = 'CRLV';
public const CARGA                  = 'Carga';
public const FOTO_VISTORIA_ANTES    = 'Foto Vistoria Antes';
public const TERMO_RESPONSABILIDADE = 'Termo de Responsabilidade';
public const FOTO_OCORRENCIA        = 'Foto de Ocorrência';
```

❌ **Não** declare o nome do tipo fora do `DocTiposModel` — isso **espalha** a fonte:

```php
class TermoService
{
    private const MODELO_TIPO = 'Modelo Termo de Responsabilidade';   // ❌ tipo fora do model
    // ...
    $tipoId = (new DocTiposModel())->getIdByNome(self::MODELO_TIPO);  // ❌ + new inline (§4.10)
}
```

✅ Declare no `DocTiposModel` e referencie a constante:

```php
$tipoId = $this->docTiposModel->getIdByNome(DocTiposModel::MODELO_TERMO_RESPONSABILIDADE);
```

**Adicionar um tipo novo:**

1. Constante no `DocTiposModel`
2. **Migration idempotente** inserindo em `doc_tipos` (checa o `nome` antes de inserir)

> ⚠️ **Não** acrescente o tipo novo ao `DocTiposSeeder`: ele mantém só o batch original.
> Somar nos dois **duplica** o tipo no `migrate:refresh` (as migrations rodam **antes** do
> seed). Tipos novos entram **só** por migration.
> Referência: `2026-07-09-100500_InsertDocTipoTermoResponsabilidade.php`.

### 9.2 Anexos — sempre pelo DocumentoService

`app/Services/DocumentoService.php`:

| Método | Uso |
|---|---|
| `saveDocumentoComReferencia(array $data, bool $mantemHistorico = true): ?int` | Salva o arquivo + vincula por referência; devolve o ID |
| `getDocumentosByReferencia(string $tabela, int $idRef): array` | Todos os anexos de um registro |
| `getDocumentoByReferenciaAndTipo(string $tabela, int $idRef, int $tipoId): ?object` | O anexo de um tipo específico |
| `deleteDocumento(int $docId): bool` | Apaga **arquivo físico + referências + registro** |

```php
$this->documentoService->saveDocumentoComReferencia([
    'file'            => $file,                       // UploadedFile
    'descricao'       => 'Foto da ocorrência',
    'tipo_id'         => $tipoId,                     // via DocTiposModel::CONSTANTE
    'visibilidade_id' => $visId,                      // via DocVisibilidadeModel::PUBLICO
    'tabela'          => 'trnsp_bdt_ocorrencias',     // tabela do registro dono
    'referencia_id'   => $ocId,                       // id do registro dono
    'user_id'         => $this->transporteContext['userId'] ?? null,
], true);
```

- **`mantemHistorico`**: `false` → **substitui** o anexo daquele tipo (ex.: documento-modelo
  único); `true` → **acumula** (ex.: galeria de fotos).
- Imagens viram **WebP** automaticamente; o arquivo vai para
  `writable/uploads/documentos/{tabela}/{referencia_id}/`.
- **Ao excluir o registro dono, remova os anexos** — `getDocumentosByReferencia(...)` +
  `deleteDocumento(...)` de cada um. Sem isso ficam **arquivos órfãos** em disco.
- Exibir: `base_url('documentos/view/' . $doc->id)` (inline) ou `documentos/download/...`.

> Referências reais: `ManutencoesController::saveFotosVistoriaAntes()` (fotos acumuladas) e
> `TermoService::saveModelo()` (substituição sem histórico).

---

## 10. Referências dentro do projeto

Exemplos reais para copiar e adaptar:

| Padrão | Arquivo de referência |
|---|---|
| Service **sem** Repository | `app/Services/e_Prefeitura/BlocoService.php` |
| Service **com** Repository | `app/Services/e_Prefeitura/EspacoService.php` |
| Repository | `app/Repositories/e_Prefeitura/EspacoRepository.php` |
| DTO Create | `app/DTOs/e_Prefeitura/Bloco/CreateBlocoDTO.php` |
| DTO Update | `app/DTOs/e_Prefeitura/Bloco/UpdateBlocoDTO.php` |
| BaseAuditedCrudService (auditoria automática) | `app/Services/e_Prefeitura/Common/Audit/BaseAuditedCrudService.php` |
| AuditLogger (auditoria manual) | `app/Services/e_Prefeitura/Common/Audit/AuditLogger.php` |
| Auditoria — uso manual (key/value) | `app/Services/e_Transporte/ConfiguracaoService.php` |
| Auditoria — tela admin | `app/Views/e_Prefeitura/admin/audit_logs_index.php` |
| Model simples | `app/Models/e_Prefeitura/BlocoModel.php` |
| Controller thin | `app/Controllers/e_Prefeitura/Admin/BlocoController.php` |
| Migration | `app/Database/Migrations/2026-04-10-140000_CreateEspacosRegrasAgendamento.php` |
| View index | `app/Views/e_Prefeitura/admin/bloco/index.php` |
| View form | `app/Views/e_Prefeitura/admin/bloco/form.php` |
| Filtro de auth | `app/Filters/EspacoFisicoAuthFilter.php` |
| Anexos — service único (§9.2) | `app/Services/DocumentoService.php` |
| Tipos de documento — fonte única (§9.1) | `app/Models/e_Documentos/DocTiposModel.php` |
| Tipo de documento — migration idempotente | `app/Database/Migrations/2026-07-09-100500_InsertDocTipoTermoResponsabilidade.php` |
| Papéis — constantes do módulo (§6.1) | `app/Constants/TransporteRoles.php` |
| Papel → rota, no filtro (§6.1) | `app/Filters/TransporteAuthFilter.php` |
| SweetAlert2 — a lib no projeto (§4.11) | `public/assets/e_EspacoFisico/vendor/sweet-alert/sweetalert2@11.js` |
| Confirmação `[data-confirm]` — padrão real (§4.11) | `app/Views/e_Transporte/bdt/folha.php` |
| Obrigatórios — validador genérico dos NOVOS (§4.12) | `public/assets/e_Prefeitura/js/form-validate.js` |
| Obrigatórios — legado complexo (§4.12) | `app/Views/e_Transporte/requests/index.php` + `…/js/requests/validate.js` |
| Preview de imagem antes de enviar (§4.13) | `public/assets/e_Transporte/js/manutencao/script.js` |
| Galeria das já salvas — modal+carrossel (§4.13) | `app/Views/e_Transporte/bdt/folha.php` (ocorrências) |
| Botão-link — `<button>` dentro do `<a>` (§4.14) | `app/Views/e_Transporte/admin/relatorios/entidade.php` |