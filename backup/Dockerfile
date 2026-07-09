FROM debian:bookworm-slim

# Alpine's mysql-client (musl) no trae el plugin caching_sha2_password.so que
# usa MySQL 8 por defecto, y mysqldump falla al conectar. El cliente MariaDB
# de Debian sí lo incluye.
ENV TZ=America/Argentina/Cordoba

RUN apt-get update && apt-get install -y --no-install-recommends \
      bash default-mysql-client rclone cron tzdata ca-certificates \
    && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone \
    && rm -rf /var/lib/apt/lists/*

COPY backup.sh /backup.sh
COPY crontab /etc/cron.d/backup-cron
RUN chmod +x /backup.sh && chmod 0644 /etc/cron.d/backup-cron

CMD ["cron", "-f"]
