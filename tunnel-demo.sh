#!/bin/bash
# ============================================================================
# tunnel-demo.sh -- Expone el laboratorio a internet via trycloudflare
#                    (Cloudflare Quick Tunnel) y avisa la URL por ntfy.
#
# Uso:
#   ./tunnel-demo.sh                    # topic ntfy random
#   ./tunnel-demo.sh mi-topic-secreto   # topic fijo (elegi algo dificil de adivinar)
#
# Antes de correrlo: suscribite al topic en la app ntfy (o abri
# https://ntfy.sh/<topic> en el navegador del celular) para recibir el aviso.
#
# Para cortar la demo: Ctrl+C (el tunel se cae y la URL deja de andar).
# ============================================================================
set -euo pipefail

TOPIC="${1:-gs-lab-demo-$(openssl rand -hex 4)}"

echo "════════════════════════════════════════════════════"
echo "  Topic ntfy: $TOPIC"
echo "  Suscribite en: https://ntfy.sh/$TOPIC"
echo "════════════════════════════════════════════════════"
echo ""

cloudflared tunnel --url http://localhost:80 2>&1 | while IFS= read -r line; do
  echo "$line"
  if [[ "$line" == *"trycloudflare.com"* ]]; then
    URL=$(echo "$line" | grep -oE 'https://[a-zA-Z0-9.-]+\.trycloudflare\.com' || true)
    if [ -n "$URL" ]; then
      curl -s -d "🦷 Demo Laboratorio GS lista: $URL" "https://ntfy.sh/$TOPIC" > /dev/null
      echo ""
      echo "✅ URL enviada a https://ntfy.sh/$TOPIC → $URL"
      echo ""
    fi
  fi
done
