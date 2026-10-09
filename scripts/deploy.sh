#!/usr/bin/env bash
# ============================================================
# Deploy do Quadro de Carga (NBR 5410) no servidor via rsync + docker compose.
# Mesmo padrão do GSTI (scripts/deploy.sh do repositório do GSTI).
#
#   ./scripts/deploy.sh             # deploy padrão (rsync + rebuild + teste)
#   ./scripts/deploy.sh --dry-run   # mostra o que seria enviado, sem enviar
#
# O host do servidor NÃO fica no repositório. Crie scripts/servidor.local (ignorado pelo git;
# modelo em scripts/servidor.local.example) ou passe pelo ambiente (o ambiente tem prioridade):
#   REMOTE_HOST   usuário@host        obrigatório
#   REMOTE_DIR    pasta no servidor   default: quadro-de-carga (relativa à home do usuário)
#
# App: http://<host>/quadro-de-carga/  (rota no Caddy do GSTI — ver docs/DEPLOY.md)
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE="docker compose -f docker-compose.prod.yml"

# Config local do servidor (fora do git e fora do rsync). O ambiente tem prioridade sobre o arquivo.
_env_host="${REMOTE_HOST:-}"; _env_dir="${REMOTE_DIR:-}"
if [ -f "$SCRIPT_DIR/servidor.local" ]; then
    # shellcheck disable=SC1091
    . "$SCRIPT_DIR/servidor.local"
fi
REMOTE_HOST="${_env_host:-${REMOTE_HOST:-}}"
REMOTE_DIR="${_env_dir:-${REMOTE_DIR:-quadro-de-carga}}"

if [ -z "$REMOTE_HOST" ]; then
    echo "REMOTE_HOST não definido. Copie scripts/servidor.local.example para scripts/servidor.local" >&2
    echo "e ajuste, ou rode: REMOTE_HOST=usuario@host $0" >&2
    exit 1
fi

# rsync --delete apaga o que não existe na origem: nunca aponte para uma pasta genérica
# (vazia, '.', '..', '~', '/', ou absoluta com até 2 níveis, ex.: /home/usuario).
dir="${REMOTE_DIR%/}"
if [ -z "$dir" ] || [ "$dir" = "." ] || [ "$dir" = ".." ] || [ "$dir" = "~" ] \
   || [[ "$dir" =~ ^/([^/]+(/[^/]+)?)?$ ]]; then
    echo "REMOTE_DIR inseguro para rsync --delete: '$REMOTE_DIR'" >&2
    exit 1
fi

EXTRA_FLAGS=()
DRY_RUN=0
for arg in "$@"; do
    case "$arg" in
        --dry-run)    DRY_RUN=1; EXTRA_FLAGS+=("--dry-run" "-v") ;;
        -v|--verbose) EXTRA_FLAGS+=("-v") ;;
        -h|--help)    sed -n '2,/^# ===/p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) echo "flag desconhecida: $arg" >&2; exit 1 ;;
    esac
done

# Fica de fora do servidor: VCS, dependências/builds locais, documentos do projeto (docs/ tem
# arquivos de clientes e não é usado no build) e segredos (.env do servidor é preservado).
EXCLUDES=(
    --exclude '.git/' --exclude '.claude/' --exclude '.vscode/' --exclude '.idea/'
    --exclude 'node_modules/' --exclude 'dist/' --exclude 'target/'
    --exclude 'docs/'
    --exclude '.env' --exclude '.env.local' --exclude '*.local'
    --exclude '.DS_Store'
)

cd "$(dirname "$0")/.."

echo "Deploy do Quadro de Carga"
echo "  origem:  $(pwd)/"
echo "  destino: ${REMOTE_HOST}:${REMOTE_DIR}/"
echo

ssh "$REMOTE_HOST" "mkdir -p '$REMOTE_DIR'"

echo "Sincronizando arquivos..."
rsync -az --delete --human-readable "${EXTRA_FLAGS[@]}" "${EXCLUDES[@]}" ./ "${REMOTE_HOST}:${REMOTE_DIR}/"

if [ "$DRY_RUN" -eq 1 ]; then
    echo "dry-run: nada enviado."
    exit 0
fi

echo
echo "Verificando pré-requisitos no servidor..."
ssh "$REMOTE_HOST" bash -s -- "$REMOTE_DIR" <<'REMOTE'
set -euo pipefail
dir="$1"
cd "$dir"

# A rede do proxy do GSTI precisa existir: é por ela que o Caddy alcança o contêiner web.
if ! docker network inspect gsti_default >/dev/null 2>&1; then
    echo "Rede 'gsti_default' não existe: suba o GSTI (proxy Caddy) antes deste deploy." >&2
    exit 1
fi

# .env só é criado na primeira vez (e preservado depois). A senha do banco nunca sai do servidor.
if [ ! -f .env ]; then
    umask 077
    printf 'DB_PASSWORD=%s\n' "$(openssl rand -hex 24)" > .env
    echo ".env criado com DB_PASSWORD aleatória."
fi

# Pasta dos backups: fora da pasta do app (o rsync --delete não a toca) e criada aqui porque o
# cron redireciona o log para dentro dela antes de o backup.sh rodar (ver docs/DEPLOY.md).
mkdir -p "$HOME/quadro-de-carga-backups"
chmod 700 "$HOME/quadro-de-carga-backups"
REMOTE

echo
echo "Subindo serviços no servidor (build pode levar alguns minutos)..."
ssh "$REMOTE_HOST" "cd '$REMOTE_DIR' && $COMPOSE up -d --build"

echo
echo "Aguardando a API responder (web -> api -> banco)..."
ok=0
for _ in $(seq 1 45); do
    if ssh "$REMOTE_HOST" "docker exec quadro-de-carga-web wget -qO- http://127.0.0.1/api/quadros >/dev/null 2>&1"; then
        ok=1
        break
    fi
    sleep 4
done
if [ "$ok" -ne 1 ]; then
    echo "A API não respondeu a tempo. Logs:" >&2
    ssh "$REMOTE_HOST" "cd '$REMOTE_DIR' && $COMPOSE logs --tail=40 api web" >&2 || true
    exit 1
fi
echo "API respondendo."

echo
echo "Testando a rota pública (Caddy do GSTI)..."
codigo="$(ssh "$REMOTE_HOST" "curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1/quadro-de-carga/")"
if [ "$codigo" = "200" ]; then
    echo "Deploy concluído."
    echo "  App: http://${REMOTE_HOST#*@}/quadro-de-carga/"
else
    echo "Rota /quadro-de-carga/ respondeu HTTP $codigo no Caddy (esperado 200)." >&2
    echo "Os contêineres estão no ar, mas falta o bloco da rota no Caddyfile do GSTI:" >&2
    echo "veja docs/DEPLOY.md (seção 'Rota no Caddy')." >&2
    exit 1
fi
