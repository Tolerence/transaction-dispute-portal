#!/usr/bin/env bash
# Dumps the dispute_portal database to backups/ as gzipped SQL. Requires the db container to be running.
# Backups contain customer data: keep the backups/ folder off shared drives (it is gitignored).
set -euo pipefail

cd "$(dirname "$0")/.."
mkdir -p backups

stamp="$(date -u +%Y%m%dT%H%M%SZ)"
out="backups/dispute_portal-${stamp}.sql.gz"

docker compose exec -T db pg_dump -U dispute --no-owner --no-privileges dispute_portal | gzip > "$out"

# Refuse to keep an empty dump: an empty file means pg_dump produced nothing.
if [ ! -s "$out" ]; then
  rm -f "$out"
  echo "backup failed: empty dump" >&2
  exit 1
fi

echo "wrote $out ($(du -h "$out" | cut -f1))"
