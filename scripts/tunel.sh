#!/usr/bin/env bash
#
# Gerencia o túnel Cloudflare (quick tunnel) que expõe o app na internet,
# para um amigo testar de fora sem depender da sessão do Claude.
#
# Uso (no terminal do WSL/Ubuntu, o mesmo onde roda o docker):
#   ./scripts/tunel.sh start     # sobe o túnel e imprime o link
#   ./scripts/tunel.sh link      # só imprime o link atual
#   ./scripts/tunel.sh status    # mostra se está rodando + link
#   ./scripts/tunel.sh stop      # derruba o túnel (o link para de funcionar)
#   ./scripts/tunel.sh restart   # derruba e sobe de novo (gera link NOVO)
#   ./scripts/tunel.sh logs      # últimas linhas do log do cloudflared
#
# Variáveis opcionais: PORTA (padrão 8085), CLOUDFLARED (caminho do binário).
#
set -uo pipefail

PORTA="${PORTA:-8085}"
ALVO="http://localhost:${PORTA}"

CLOUDFLARED="${CLOUDFLARED:-$HOME/.local/bin/cloudflared}"
[ -x "$CLOUDFLARED" ] || CLOUDFLARED="$(command -v cloudflared 2>/dev/null || true)"

DIR="$HOME/.cache/quadro-de-carga"
LOG="$DIR/cloudflared.log"
PIDFILE="$DIR/cloudflared.pid"
URL_RE='https://[a-z0-9-]+\.trycloudflare\.com'
mkdir -p "$DIR"

# PID do cloudflared em execução (via pidfile ou por padrão do comando); vazio se parado.
_pid_rodando() {
  local pid
  if [ -f "$PIDFILE" ]; then
    pid="$(cat "$PIDFILE" 2>/dev/null)"
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then echo "$pid"; return; fi
  fi
  pgrep -f "cloudflared tunnel --url ${ALVO}" 2>/dev/null | head -1
}

_link() { grep -oE "$URL_RE" "$LOG" 2>/dev/null | tail -1; }

cmd_start() {
  if [ -z "${CLOUDFLARED:-}" ] || [ ! -x "$CLOUDFLARED" ]; then
    echo "ERRO: cloudflared não encontrado. Ajuste a variável CLOUDFLARED ou instale o binário." >&2
    exit 1
  fi

  local pid; pid="$(_pid_rodando)"
  if [ -n "$pid" ]; then
    echo "Túnel já está rodando (PID $pid)."
    echo "Link: $(_link)"
    return 0
  fi

  if ! curl -s -o /dev/null --max-time 5 "$ALVO/"; then
    echo "Aviso: o app não respondeu em $ALVO. Suba com 'docker compose up -d' na pasta do projeto." >&2
    echo "       Vou iniciar o túnel mesmo assim (o link só serve quando o app estiver no ar)." >&2
  fi

  : > "$LOG"
  nohup "$CLOUDFLARED" tunnel --url "$ALVO" --no-autoupdate >> "$LOG" 2>&1 &
  echo $! > "$PIDFILE"

  local url="" i
  for i in $(seq 1 25); do
    url="$(_link)"
    [ -n "$url" ] && break
    sleep 1
  done

  if [ -z "$url" ]; then
    echo "Túnel iniciado, mas a URL ainda não apareceu. Veja '$0 logs'." >&2
    exit 1
  fi

  echo "Túnel no ar (PID $(cat "$PIDFILE"))."
  echo "Link para enviar: $url"

  # O edge da Cloudflare leva ~30-60s para rotear um subdomínio novo. Só avisa
  # "pronto" quando o link responder de fato (evita compartilhar um link que ainda cai).
  printf "Aguardando o link ficar acessível"
  local pronto=0 code
  for i in $(seq 1 25); do
    code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 6 "$url/" 2>/dev/null)"
    if [ "$code" = "200" ]; then pronto=1; break; fi
    printf "."
    sleep 3
  done
  echo
  if [ "$pronto" = 1 ]; then
    echo "Pronto! O link já responde — pode enviar ao seu amigo."
  else
    echo "O link ainda não respondeu. Confirme que o app está de pé (docker compose up -d) e rode '$0 status' em ~1 min." >&2
  fi
}

cmd_stop() {
  local pid; pid="$(_pid_rodando)"
  if [ -z "$pid" ]; then
    echo "Nenhum túnel em execução."
    rm -f "$PIDFILE"
    return 0
  fi
  pkill -TERM -f "cloudflared tunnel --url ${ALVO}" 2>/dev/null
  [ -n "$pid" ] && kill -TERM "$pid" 2>/dev/null
  sleep 1
  pkill -KILL -f "cloudflared tunnel --url ${ALVO}" 2>/dev/null
  rm -f "$PIDFILE"
  if [ -n "$(_pid_rodando)" ]; then
    echo "ERRO: não consegui encerrar o túnel." >&2
    exit 1
  fi
  echo "Túnel encerrado — o link parou de funcionar."
}

cmd_status() {
  local pid; pid="$(_pid_rodando)"
  if [ -n "$pid" ]; then
    echo "rodando (PID $pid)"
    echo "link: $(_link)"
  else
    echo "parado"
  fi
}

case "${1:-}" in
  start)   cmd_start ;;
  stop)    cmd_stop ;;
  restart) cmd_stop; sleep 1; cmd_start ;;
  status)  cmd_status ;;
  link)    _link ;;
  logs)    tail -n 40 "$LOG" 2>/dev/null || echo "(sem log ainda)" ;;
  *) echo "Uso: $0 {start|stop|restart|status|link|logs}"; exit 2 ;;
esac
