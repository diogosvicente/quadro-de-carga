# Deploy no servidor

O sistema roda num servidor Docker da UERJ em `http://<servidor>/quadro-de-carga/`, no mesmo padrão
do GSTI (`http://<servidor>/gsti/`): Docker Compose + proxy Caddy na porta 80, uma subpasta por sistema.
O endereço do servidor **não fica neste repositório**: ele é lido de `scripts/servidor.local` (veja abaixo).

## Configurar o servidor (uma vez, em cada máquina que faz deploy)

Host e usuário SSH ficam fora do git. Copie o modelo e ajuste:

```bash
cp scripts/servidor.local.example scripts/servidor.local
# edite: REMOTE_HOST=usuario@host
```

`scripts/servidor.local` é ignorado pelo git (`*.local`) e excluído do rsync: não vai para o GitHub nem para o
servidor. `deploy.sh` e `backup-pull.sh` leem esse arquivo; variáveis de ambiente
(`REMOTE_HOST=usuario@host ./scripts/deploy.sh`) têm prioridade. É preciso acesso SSH por chave ao servidor.

## Como funciona

```
navegador ──:80──> gsti-proxy (Caddy, compose do GSTI)
                     ├─ /gsti/*             -> gsti-app
                     └─ /quadro-de-carga/*  -> quadro-de-carga-web (nginx) ──> quadro-de-carga-api ──> quadro-de-carga-db
```

- O Caddy tira o prefixo (`handle_path`), então o nginx do `web` continua servindo na raiz.
- O front é **buildado com `base=/quadro-de-carga/`** (`VITE_BASE_PATH` em `docker-compose.prod.yml`);
  sem a variável (dev local e `docker-compose.yml`), o app continua na raiz `/`.
- O compose de produção (`docker-compose.prod.yml`) **não publica porta**: só o `web` entra na
  rede `gsti_default` para o Caddy alcançá-lo. `db` e `api` ficam na rede interna do compose.
- Pasta no servidor: `~/quadro-de-carga`. Segredo: `.env` ali (só `DB_PASSWORD`, gerado no primeiro
  deploy, `chmod 600`, nunca vai para o git nem é sobrescrito pelo rsync).
- Dado persistente: volume Docker `quadro-de-carga_pgdata`. Backups em `~/quadro-de-carga-backups`
  (fora da pasta do app, porque o deploy usa `rsync --delete`).

## Fazer deploy (atualizar)

```bash
cd ~/rodrigo
./scripts/deploy.sh --dry-run   # opcional: mostra o que seria enviado
./scripts/deploy.sh             # rsync + docker compose up -d --build + teste da API e da rota
```

**Pelo Windows** (PowerShell aberto na pasta do projeto no WSL, `\\wsl.localhost\<distro>\home\<usuario>\rodrigo`):
o script precisa rodar *dentro do WSL* (é lá que estão `rsync` e a chave SSH), então use o `wsl`:

```powershell
cd \\wsl.localhost\<distro>\home\<usuario>\rodrigo
wsl ./scripts/deploy.sh
```

(O `wsl` abre o shell na mesma pasta Linux. Rodar o `.sh` direto no PowerShell/CMD não funciona: o Windows não tem `rsync`.)

O build roda **no servidor** (as imagens usam `network: host` no build porque o servidor só
tem DNS IPv6, igual ao GSTI). O script espera a API responder e termina testando
`http://127.0.0.1/quadro-de-carga/` no servidor.

Cada deploy **recria `api` e `web`** (o Docker 29 grava o horário do build na imagem, então o ID muda mesmo
sem alteração de código): o app fica ~15 s fora do ar. O banco e seus dados não são tocados.

## Rota no Caddy (feita uma vez)

A porta 80 é do proxy do GSTI, então o sistema só aparece depois de existir este bloco no `docker/Caddyfile`
do GSTI (dentro de `:80 { ... }`, antes do `handle` de 404):

```caddy
	redir /quadro-de-carga /quadro-de-carga/ 308

	handle_path /quadro-de-carga/* {
		reverse_proxy quadro-de-carga-web:80
	}
```

Atenção: o deploy do GSTI usa `rsync --delete` e **sobrescreve** esse Caddyfile com o do repositório
do GSTI. O mesmo bloco precisa estar no `docker/Caddyfile` do repo do GSTI (e commitado lá),
senão o próximo deploy do GSTI remove a rota. Para aplicar sem derrubar o GSTI:

```bash
ssh usuario@host 'docker exec gsti-proxy caddy validate --config /etc/caddy/Caddyfile \
  && docker exec gsti-proxy caddy reload --config /etc/caddy/Caddyfile'
```

(`reload` é graceful: se a config for inválida, o Caddy mantém a anterior no ar.)

## Operação

```bash
ssh usuario@host
cd ~/quadro-de-carga
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f api        # ou web / db
docker compose -f docker-compose.prod.yml restart api
docker compose -f docker-compose.prod.yml down               # para tudo (mantém o banco)
```

## Backup (igual ao GSTI)

- **No servidor, todo dia às 02:40 (Brasília)**: `scripts/backup.sh`, via cron do usuário do deploy (convive com o
  job do GSTI, 02:30). Gera `~/quadro-de-carga-backups/db-AAAAMMDD-HHMMSS.sql.gz`, confere o dump e apaga o que tem
  mais de 30 dias. Log em `~/quadro-de-carga-backups/backup.log`.
- **Cópia fora do servidor**, na sua máquina (não apaga nada localmente):

  ```bash
  ./scripts/backup-pull.sh        # copia para ~/quadro-de-carga-backups
  ```

- **Restaurar** (substitui o conteúdo atual do banco; rode no servidor, na pasta do projeto):

  ```bash
  gunzip -c ~/quadro-de-carga-backups/db-AAAAMMDD-HHMMSS.sql.gz \
    | docker compose -f docker-compose.prod.yml exec -T db sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
  ```

- **Instalar o cron num servidor novo** (uma vez; o `deploy.sh` já cria a pasta de backups):

  ```bash
  ( crontab -l; echo '40 5 * * * $HOME/quadro-de-carga/scripts/backup.sh >> $HOME/quadro-de-carga-backups/backup.log 2>&1' ) | crontab -
  ```

  (05:40 UTC = 02:40 de Brasília; o servidor roda em UTC. Faça cópia do `crontab -l` antes de editar: o arquivo é
  compartilhado com os jobs de outros sistemas.)

## Limitações conhecidas

- **Sem autenticação real**: o login do front é ilustrativo (`frontend/src/auth.ts`) e a API não exige
  credencial. Qualquer pessoa que alcance o servidor cria, edita e apaga quadros: mantenha o acesso restrito à rede
  interna e não coloque dados sensíveis até haver autenticação (previsto em ARCHITECTURE.md §8).
- HTTP puro, sem TLS (igual ao GSTI hoje). Para HTTPS, ver o comentário no topo do Caddyfile.
