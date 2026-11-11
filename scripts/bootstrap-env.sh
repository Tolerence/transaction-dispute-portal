#!/usr/bin/env bash
# Creates .env from .env.example (if missing) and generates any empty local secrets.
# Existing values are never overwritten. Requires openssl and perl (both ship with macOS and Linux).
set -euo pipefail

cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  cp .env.example .env
  echo "Created .env from .env.example"
fi

fill() {  # fill KEY GENERATOR_COMMAND  (adds the key if the file predates it)
  local key="$1" cmd="$2"
  grep -qE "^${key}=" .env || echo "${key}=" >> .env
  if grep -qE "^${key}=$" .env; then
    local value
    value="$(eval "$cmd")"
    # Pass the value through the environment: base64 secrets contain "/", which would end a
    # sed or perl substitution written with the value inline.
    FILL_VALUE="$value" perl -0pi -e "s/^${key}=\$/${key}=\$ENV{FILL_VALUE}/m" .env
    echo "Generated ${key}"
  fi
}

fill DB_PASSWORD 'openssl rand -hex 16'
fill JWT_SECRET 'openssl rand -base64 48 | tr -d "\n"'
fill KEYCLOAK_ADMIN_PASSWORD 'openssl rand -hex 16'
echo ".env is ready. Review it before first use."
