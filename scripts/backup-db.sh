#!/usr/bin/env bash
#
# Timestamped MySQL backup for the Hospital Management System.
#
#   ./scripts/backup-db.sh                    # back up the docker-compose database
#   DB_HOST=db.internal ./scripts/backup-db.sh
#
# Restore with:
#   ./scripts/restore-db.sh backups/hospital_db-20260923-1430.sql.gz
#
# Schedule it with cron, e.g. every night at 02:00:
#   0 2 * * * cd /opt/hms && ./scripts/backup-db.sh >> /var/log/hms-backup.log 2>&1

set -euo pipefail

DB_NAME="${DB_NAME:-hospital_db}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root_password}"
BACKUP_DIR="${BACKUP_DIR:-backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

# By default talk to the compose container, which avoids needing a MySQL client
# installed on the host. Set DB_HOST to back up a server directly instead.
CONTAINER="${MYSQL_CONTAINER:-hms-mysql}"
DB_HOST="${DB_HOST:-}"

mkdir -p "$BACKUP_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
TARGET="${BACKUP_DIR}/${DB_NAME}-${STAMP}.sql.gz"

echo "Backing up '${DB_NAME}' to ${TARGET}"

# --single-transaction takes a consistent snapshot without locking the tables,
# so the application keeps serving while the backup runs.
DUMP_ARGS=(
  --single-transaction
  --routines
  --triggers
  --events
  --default-character-set=utf8mb4
  "$DB_NAME"
)

if [ -n "$DB_HOST" ]; then
  mysqldump -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASSWORD" "${DUMP_ARGS[@]}" | gzip > "$TARGET"
else
  docker exec "$CONTAINER" mysqldump -u "$DB_USER" -p"$DB_PASSWORD" "${DUMP_ARGS[@]}" \
    | gzip > "$TARGET"
fi

# A dump that failed midway can still leave a small, valid-looking gzip file,
# so check the result is plausible rather than trusting the exit code alone.
SIZE=$(wc -c < "$TARGET")
if [ "$SIZE" -lt 1024 ]; then
  echo "ERROR: backup is only ${SIZE} bytes - treating it as failed" >&2
  rm -f "$TARGET"
  exit 1
fi

echo "Backup complete: ${TARGET} (${SIZE} bytes)"

echo "Removing backups older than ${RETENTION_DAYS} days"
find "$BACKUP_DIR" -name "${DB_NAME}-*.sql.gz" -mtime "+${RETENTION_DAYS}" -print -delete

echo "Done."
