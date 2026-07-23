#!/usr/bin/env bash
#
# Gerencia o túnel Cloudflare (quick tunnel) que expõe o app na internet,
# para um amigo testar de fora sem depender da sessão do Claude.
#
# Uso (no terminal do WSL/Ubuntu, o mesmo onde roda o docker):
#   ./scripts/tunel.sh start     # sobe o túnel (ou reinicia se o atual caiu) e imprime o link
#   ./scripts/tunel.sh link      # só imprime o link atual
#   ./scripts/tunel.sh status    # online / OFFLINE (caiu) / parado
#   ./scripts/tunel.sh stop      # derruba o túnel (o link para de funcionar)
#   ./scripts/tunel.sh restart   # derruba e sobe de novo (gera link NOVO)
#   ./scripts/tunel.sh logs      # últimas linhas do log do cloudflared
#
# Variáveis opcionais: PORTA (padrão 8085), CLOUDFLARED (caminho), PROTOCOLO (padrão http2).
#
# Nota: "quick tunnel" é efêmero — a conexão pode cair (aí o subdomínio some, dá NXDOMAIN)
# e cada (re)início gera um link novo. Para um link FIXO e estável seria preciso um
# "named tunnel" com conta Cloudflare. Este script detecta a queda e reinicia sozinho.
#
set -uo pipefail

PORTA="${PORTA:-8085}"
ALVO="http://localhost:${PORTA}"
# QUIC (UDP) é instável no WSL/redes corporativas e derruba o túnel; http2 (TCP 443) é mais estável.
PROTOCOLO="${PROTOCOLO:-http2}"

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

# Código HTTP do link atual (000 = não resolveu/conectou → túnel caído/NXDOMAIN).
# -4 força IPv4: o WSL2 costuma ter IPv6 quebrado e o subdomínio Cloudflare resolve p/ IPv6,
# o que daria falso "offline" no health-check (o navegador Windows do amigo não tem esse problema).
_codigo_link() {
  local url code
  url="$(_link)"
  [ -n "$url" ] || { echo "000"; return; }
  # curl -w já imprime "000" quando não conecta; não duplicar com "|| echo".
  code="$(curl -4 -s -o /dev/null -w '%{http_code}' --max-time 8 "$url/" 2>/dev/null)"
  echo "${code:-000}"
}

# Túnel vivo no edge da Cloudflare = o link resolve e conecta (qualquer HTTP, mesmo 502).
_tunel_vivo() { [ "$(_codigo_link)" != "000" ]; }

_iniciar() {
  : > "$LOG"
  nohup "$CLOUDFLARED" tunnel --url "$ALVO" --protocol "$PROTOCOLO" --no-autoupdate >> "$LOG" 2>&1 &
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

  echo "Túnel no ar (PID $(cat "$PIDFILE"), protocolo $PROTOCOLO)."
  echo "Link para enviar: $url"

  # O edge da Cloudflare leva ~30-60s para rotear um subdomínio novo.
  printf "Aguardando o link ficar acessível"
  local pronto=0 i2
  for i2 in $(seq 1 25); do
    [ "$(_codigo_link)" = "200" ] && { pronto=1; break; }
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

cmd_start() {
  if [ -z "${CLOUDFLARED:-}" ] || [ ! -x "$CLOUDFLARED" ]; then
    echo "ERRO: cloudflared não encontrado. Ajuste a variável CLOUDFLARED ou instale o binário." >&2
    exit 1
  fi

  local pid; pid="$(_pid_rodando)"
  if [ -n "$pid" ]; then
    if _tunel_vivo; then
      echo "Túnel já está rodando e acessível (PID $pid)."
      echo "Link: $(_link)"
      return 0
    fi
    echo "Há um túnel rodando (PID $pid) mas o link NÃO responde — caiu (NXDOMAIN). Reiniciando…" >&2
    cmd_stop >/dev/null 2>&1
  fi

  if ! curl -s -o /dev/null --max-time 5 "$ALVO/"; then
    echo "Aviso: o app não respondeu em $ALVO. Suba com 'docker compose up -d' na pasta do projeto." >&2
    echo "       Vou iniciar o túnel mesmo assim (o link só serve quando o app estiver no ar)." >&2
  fi
  _iniciar
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
  if [ -z "$pid" ]; then
    echo "parado"
    return 0
  fi
  local codigo; codigo="$(_codigo_link)"
  if [ "$codigo" = "000" ]; then
    echo "OFFLINE (PID $pid) — o túnel caiu; rode '$0 restart' (ou '$0 start') para um link novo."
  elif [ "$codigo" = "200" ]; then
    echo "online (PID $pid) — $(_link)"
  else
    echo "no ar (PID $pid, HTTP $codigo) — o túnel responde, mas o app não (suba: docker compose up -d). Link: $(_link)"
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
