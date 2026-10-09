#!/usr/bin/env bash
# ============================================================
# Backup do Quadro de Carga: dump do banco PostgreSQL.
# Roda no servidor (cron diário). Mantém RETENCAO_DIAS dias.
# Mesmo padrão do GSTI (scripts/backup.sh do repositório do GSTI).
#
#   scripts/backup.sh
#   BACKUP_DIR=/outro/lugar RETENCAO_DIAS=60 scripts/backup.sh
#
# Restaurar (de dentro da pasta do projeto no servidor; SUBSTITUI o conteúdo atual do banco):
#   gunzip -c ~/quadro-de-carga-backups/db-AAAAMMDD-HHMMSS.sql.gz | docker compose -f docker-compose.prod.yml exec -T db sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
# ============================================================

set -euo pipefail

APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
# Fora da pasta do app de propósito: o deploy usa rsync --delete e não pode apagar backups.
BACKUP_DIR="${BACKUP_DIR:-$HOME/quadro-de-carga-backups}"
RETENCAO_DIAS="${RETENCAO_DIAS:-30}"
STAMP="$(TZ=America/Sao_Paulo date +%Y%m%d-%H%M%S)"

DB_FILE="$BACKUP_DIR/db-$STAMP.sql.gz"

log() { echo "[$(TZ=America/Sao_Paulo date '+%F %T')] $*"; }

# Não deixa arquivo pela metade se algo falhar no meio.
trap 'rm -f "$DB_FILE.tmp"' EXIT

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"
cd "$APP_DIR"

log "início do backup em $BACKUP_DIR"

# Credencial lida dentro do contêiner (socket local), nunca passa pela linha de comando do host.
# --clean --if-exists: o dump recria tudo, então restaura tanto sobre o banco atual quanto num vazio.
docker compose -f docker-compose.prod.yml exec -T db sh -c \
    'exec pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner' \
    | gzip -9 > "$DB_FILE.tmp"

gzip -t "$DB_FILE.tmp"
# Dumps novos terminam com '\unrestrict <chave>', por isso olhamos as últimas linhas, não só a última.
if ! gunzip -c "$DB_FILE.tmp" | tail -n 5 | grep -q "PostgreSQL database dump complete"; then
    log "ERRO: dump do banco incompleto"
    exit 1
fi
mv "$DB_FILE.tmp" "$DB_FILE"
chmod 600 "$DB_FILE"
log "banco ok:  $(basename "$DB_FILE") ($(du -h "$DB_FILE" | cut -f1))"

# ---- Retenção ----
removidos=$(find "$BACKUP_DIR" -maxdepth 1 -name 'db-*.sql.gz' -mtime +"$RETENCAO_DIAS" -print -delete | wc -l)
log "fim. removidos por retenção (> ${RETENCAO_DIAS} dias): $removidos"
