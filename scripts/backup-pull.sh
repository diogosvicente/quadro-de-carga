#!/usr/bin/env bash
# ============================================================
# Copia os backups do servidor para esta máquina (cópia fora do servidor).
# Não apaga nada localmente: aqui a retenção é maior que no servidor.
# Mesmo padrão do GSTI (scripts/backup-pull.sh do repositório do GSTI).
# Lê o host de scripts/servidor.local (ver scripts/deploy.sh); o ambiente tem prioridade.
#
#   ./scripts/backup-pull.sh
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
_env_host="${REMOTE_HOST:-}"; _env_bdir="${REMOTE_BACKUP_DIR:-}"
if [ -f "$SCRIPT_DIR/servidor.local" ]; then
    # shellcheck disable=SC1091
    . "$SCRIPT_DIR/servidor.local"
fi
REMOTE_HOST="${_env_host:-${REMOTE_HOST:-}}"
# Caminho relativo = a partir da home do usuário no servidor.
REMOTE_BACKUP_DIR="${_env_bdir:-${REMOTE_BACKUP_DIR:-quadro-de-carga-backups}}"
LOCAL_BACKUP_DIR="${LOCAL_BACKUP_DIR:-$HOME/quadro-de-carga-backups}"

if [ -z "$REMOTE_HOST" ]; then
    echo "REMOTE_HOST não definido. Copie scripts/servidor.local.example para scripts/servidor.local" >&2
    echo "e ajuste, ou rode: REMOTE_HOST=usuario@host $0" >&2
    exit 1
fi

mkdir -p "$LOCAL_BACKUP_DIR"
chmod 700 "$LOCAL_BACKUP_DIR"

rsync -a --human-readable --info=stats1,progress0 \
      --include 'db-*.sql.gz' --include 'backup.log' --exclude '*' \
      "$REMOTE_HOST:$REMOTE_BACKUP_DIR/" "$LOCAL_BACKUP_DIR/"

echo "Backups em $LOCAL_BACKUP_DIR:"
ls -lh "$LOCAL_BACKUP_DIR" | tail -n +2 | tail -6
