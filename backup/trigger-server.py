#!/usr/bin/env python3
"""
Mini servidor HTTP para disparar el backup a demanda (el botón de la UI).

El backup ya corre solo a las 3 AM por cron (ver crontab). Este servidor agrega
la posibilidad de correrlo AHORA sin entrar por consola: expone POST /run, que
lanza el MISMO backup.sh en segundo plano.

Seguridad: solo escucha en la red interna de Docker (gs-net) — NO se publica al
host — y además exige el header X-Backup-Key == env BACKUP_TRIGGER_KEY. El único
que lo llama es ms-auth (server-to-server), que valida antes que el usuario sea
ADMIN. La key nunca llega al navegador.

No usa frameworks: http.server de la stdlib alcanza de sobra para un endpoint.
"""
import os
import subprocess
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

PORT = int(os.environ.get("BACKUP_TRIGGER_PORT", "3002"))
KEY = os.environ.get("BACKUP_TRIGGER_KEY", "")
BACKUP_SCRIPT = "/backup.sh"

# Evita backups solapados: si ya hay uno corriendo, no lanza otro.
_lock = threading.Lock()
_corriendo = {"activo": False}


def _correr_backup():
    try:
        subprocess.run(["/bin/bash", BACKUP_SCRIPT], check=False)
    finally:
        with _lock:
            _corriendo["activo"] = False


class Handler(BaseHTTPRequestHandler):
    def _json(self, code, body):
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(body.encode("utf-8"))

    def do_GET(self):
        if self.path == "/health":
            self._json(200, '{"ok":true}')
        else:
            self._json(404, '{"error":"not found"}')

    def do_POST(self):
        if self.path != "/run":
            self._json(404, '{"error":"not found"}')
            return
        # Sin key configurada = no se permite disparar por HTTP (solo cron).
        if not KEY or self.headers.get("X-Backup-Key") != KEY:
            self._json(401, '{"error":"clave invalida"}')
            return
        with _lock:
            if _corriendo["activo"]:
                self._json(409, '{"error":"ya hay un backup en curso"}')
                return
            _corriendo["activo"] = True
        threading.Thread(target=_correr_backup, daemon=True).start()
        self._json(202, '{"ok":true,"mensaje":"Backup iniciado en segundo plano"}')

    # Silencia el log por request (ruido); los errores del backup salen en backup.log.
    def log_message(self, *args):
        pass


if __name__ == "__main__":
    print(f"[trigger] Servidor de disparo de backup escuchando en :{PORT}", flush=True)
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
