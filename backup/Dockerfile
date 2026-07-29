FROM debian:bookworm-slim

# Alpine's mysql-client (musl) no trae el plugin caching_sha2_password.so que
# usa MySQL 8 por defecto, y mysqldump falla al conectar. El cliente MariaDB
# de Debian sí lo incluye.
ENV TZ=America/Argentina/Cordoba

# python3 (mínimo) para el mini-servidor de disparo del backup a demanda.
RUN apt-get update && apt-get install -y --no-install-recommends \
      bash default-mysql-client rclone cron tzdata ca-certificates python3-minimal \
    && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone \
    && rm -rf /var/lib/apt/lists/*

COPY backup.sh /backup.sh
COPY entrypoint.sh /entrypoint.sh
COPY trigger-server.py /trigger-server.py
COPY crontab /etc/cron.d/backup-cron
RUN chmod +x /backup.sh /entrypoint.sh /trigger-server.py && chmod 0644 /etc/cron.d/backup-cron

# El entrypoint vuelca el entorno del contenedor a /etc/cron-env (cron no lo
# hereda) y después arranca cron en foreground.
CMD ["/entrypoint.sh"]
