#!/usr/bin/env bash
# run-dev.sh — sobe o ngrok, atualiza o webhook no .env e inicia a aplicação.
# Uso: ./run-dev.sh
#
# DICA (opcional): se você tiver um domínio estático no ngrok (grátis),
# defina abaixo e o link do webhook nunca muda (não precisa atualizar o .env).
#   NGROK_DOMAIN="seu-dominio.ngrok-free.app"
# Deixe vazio para gerar um link novo a cada execução (o script atualiza o .env sozinho).

set -euo pipefail
cd "$(dirname "$0")"

NGROK_DOMAIN="${NGROK_DOMAIN:-}"
ENV_FILE=".env"
NGROK_LOG="/tmp/ngrok-dev.log"

# 1. Sobe o ngrok em segundo plano
if [ -n "$NGROK_DOMAIN" ]; then
  echo "▶ Iniciando ngrok no domínio fixo $NGROK_DOMAIN ..."
  ngrok http --domain="$NGROK_DOMAIN" 8080 > "$NGROK_LOG" 2>&1 &
else
  echo "▶ Iniciando ngrok ..."
  ngrok http 8080 > "$NGROK_LOG" 2>&1 &
fi
NGROK_PID=$!
trap 'echo; echo "▶ Encerrando ngrok..."; kill "$NGROK_PID" 2>/dev/null || true' EXIT

# 2. Descobre a URL pública (via API local do ngrok em 127.0.0.1:4040)
get_url() {
  curl -s http://127.0.0.1:4040/api/tunnels \
    | grep -o '"public_url":"https://[^"]*"' \
    | head -1 \
    | sed 's/.*https:\/\///; s/"$//'
}

if [ -n "$NGROK_DOMAIN" ]; then
  PUBLIC_URL="$NGROK_DOMAIN"
else
  echo "▶ Aguardando o ngrok publicar a URL ..."
  PUBLIC_URL=""
  for _ in $(seq 1 30); do
    PUBLIC_URL="$(get_url || true)"
    [ -n "$PUBLIC_URL" ] && break
    sleep 1
  done
  if [ -z "$PUBLIC_URL" ]; then
    echo "✖ Não consegui obter a URL do ngrok. Veja o log em $NGROK_LOG"
    exit 1
  fi
fi

WEBHOOK_URL="https://$PUBLIC_URL/webhook/mercadopago"
echo "▶ Webhook: $WEBHOOK_URL"

# 3. Atualiza o .env (só se o valor for diferente)
if grep -q '^MERCADO_PAGO_WEBHOOK_URL=' "$ENV_FILE"; then
  sed -i "s|^MERCADO_PAGO_WEBHOOK_URL=.*|MERCADO_PAGO_WEBHOOK_URL=$WEBHOOK_URL|" "$ENV_FILE"
else
  printf '\nMERCADO_PAGO_WEBHOOK_URL=%s\n' "$WEBHOOK_URL" >> "$ENV_FILE"
fi

# 4. Sobe a aplicação (primeiro plano; Ctrl+C encerra tudo)
echo "▶ Iniciando a aplicação ..."
./mvnw spring-boot:run
