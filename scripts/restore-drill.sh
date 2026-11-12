#!/usr/bin/env bash
# Restore drill: restores a backup into a scratch database and checks that the row counts match the live
# database. The live database is never modified. Exits non-zero if the backup cannot be restored or counts differ.
#
# Usage: scripts/restore-drill.sh [backup-file]   (defaults to the newest file in backups/)
set -euo pipefail

cd "$(dirname "$0")/.."

backup="${1:-}"
if [ -z "$backup" ]; then
  backup="$(ls -1t backups/*.sql.gz 2>/dev/null | head -1 || true)"
fi
if [ -z "$backup" ] || [ ! -f "$backup" ]; then
  echo "no backup found; run make backup first" >&2
  exit 1
fi

scratch="dispute_restore_drill"
tables=(transactions disputes dispute_events)

count() {  # count DATABASE TABLE
  docker compose exec -T db psql -U dispute -d "$1" -Atc "select count(*) from $2"
}

cleanup() {
  docker compose exec -T db psql -U dispute -d postgres -qc "DROP DATABASE IF EXISTS $scratch" >/dev/null
}
trap cleanup EXIT

echo "restoring $backup into scratch database $scratch"
cleanup
docker compose exec -T db psql -U dispute -d postgres -qc "CREATE DATABASE $scratch"
gunzip -c "$backup" | docker compose exec -T db psql -U dispute -d "$scratch" -q -v ON_ERROR_STOP=1 >/dev/null

failed=0
for table in "${tables[@]}"; do
  live="$(count dispute_portal "$table")"
  restored="$(count "$scratch" "$table")"
  if [ "$live" = "$restored" ]; then
    echo "  ok    $table: $restored rows"
  else
    echo "  FAIL  $table: live=$live restored=$restored" >&2
    failed=1
  fi
done

if [ "$failed" -ne 0 ]; then
  echo "restore drill FAILED" >&2
  exit 1
fi
echo "restore drill passed"
