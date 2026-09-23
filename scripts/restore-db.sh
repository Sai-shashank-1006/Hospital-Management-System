#!/usr/bin/env bash
#
# Restores a backup produced by backup-db.sh.
#
#   ./scripts/restore-db.sh backups/hospital_db-20260923-143000.sql.gz
#
# This REPLACES the current contents of the database. It asks for confirmation
# first unless FORCE=1 is set.

set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: $0 <backup-file.sql.gz>" >&2
  exit 1
fi

ARCHIVE="$1"

if [ ! -f "$ARCHIVE" ]; then
  echo "ERROR: no such file: $ARCHIVE" >&2
  exit 1
fi

DB_NAME="${DB_NAME:-hospital_db}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root_password}"
CONTAINER="${MYSQL_CONTAINER:-hms-mysql}"
DB_HOST="${DB_HOST:-}"

if [ "${FORCE:-0}" != "1" ]; then
  echo "This will REPLACE everything currently in '${DB_NAME}'."
  read -r -p "Type the database name to confirm: " CONFIRM
  if [ "$CONFIRM" != "$DB_NAME" ]; then
    echo "Aborted."
    exit 1
  fi
fi

echo "Restoring ${ARCHIVE} into '${DB_NAME}'"

if [ -n "$DB_HOST" ]; then
  gunzip -c "$ARCHIVE" | mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME"
else
  gunzip -c "$ARCHIVE" | docker exec -i "$CONTAINER" \
    mysql -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME"
fi

echo "Restore complete. Restart the application so it reconnects cleanly."
