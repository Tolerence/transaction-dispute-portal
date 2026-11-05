#!/usr/bin/env bash
# Withdraws any open disputes left for the demo customer by an interrupted API test run.
#
# The API suite raises disputes on fixed demo transactions, and a transaction cannot take a new
# dispute while one is open. Running this before the suite keeps every run starting from the same state.
# Uses only the public API, so it works against the running dev stack without touching the database.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8080}"
CUSTOMER_ID="${CUSTOMER_ID:-$(awk '/customerId:/ {print $2; exit}' "$ROOT/bruno/environments/Docker.bru")}"
MAX_ROUNDS=20

token=$(curl -fsS -X POST -H 'Content-Type: application/json' \
  -d "{\"customerId\":\"$CUSTOMER_ID\",\"roles\":[]}" \
  "$BASE_URL/api/dev/token" | python3 -c 'import sys, json; print(json.load(sys.stdin)["accessToken"])')

open_dispute_ids() {
  for status in SUBMITTED UNDER_REVIEW; do
    curl -fsS -H "Authorization: Bearer $token" "$BASE_URL/api/disputes?status=$status&size=100" \
      | python3 -c 'import sys, json; [print(d["id"]) for d in json.load(sys.stdin)["content"]]'
  done
}

for round in $(seq 1 "$MAX_ROUNDS"); do
  ids=$(open_dispute_ids)
  if [ -z "$ids" ]; then
    echo "API test state is clean."
    exit 0
  fi
  for id in $ids; do
    curl -fsS -X POST -H "Authorization: Bearer $token" "$BASE_URL/api/disputes/$id/withdraw" -o /dev/null
    echo "Withdrew leftover dispute $id from an earlier run."
  done
done

echo "Could not clear open disputes after $MAX_ROUNDS rounds." >&2
exit 1
