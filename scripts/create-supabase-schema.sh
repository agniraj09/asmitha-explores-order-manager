#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Creates the Asmitha Explores tables in Supabase. NO DATA is inserted.
#
#   1. put your password in .env  ->  SUPABASE_DB_PASSWORD='...'
#   2. ./scripts/create-supabase-schema.sh
#
# Safe to run more than once (the SQL script is idempotent).
# ---------------------------------------------------------------------------
set -euo pipefail

cd "$(dirname "$0")/.."

if [[ ! -f .env ]]; then
    echo "ERROR: .env not found. Copy .env.example to .env and fill it in." >&2
    exit 1
fi

# shellcheck disable=SC1091
set -a; source .env; set +a

: "${SUPABASE_DB_HOST:?missing in .env}"
: "${SUPABASE_DB_NAME:?missing in .env}"
: "${SUPABASE_DB_USER:?missing in .env}"

if [[ -z "${SUPABASE_DB_PASSWORD:-}" ]]; then
    echo "ERROR: SUPABASE_DB_PASSWORD is empty in .env." >&2
    echo "       Supabase -> Project Settings -> Database -> Database password" >&2
    exit 1
fi

# DDL goes through the session pooler (5432), not the transaction pooler (6543).
DDL_PORT="${SUPABASE_DDL_PORT:-5432}"
SCHEMA_FILE="src/main/resources/db/schema-postgres.sql"

echo "==> Creating schema on ${SUPABASE_DB_HOST}:${DDL_PORT}/${SUPABASE_DB_NAME} as ${SUPABASE_DB_USER}"

export PGPASSWORD="$SUPABASE_DB_PASSWORD"
export PGSSLMODE=require
export PGCONNECT_TIMEOUT=15

PSQL=(psql --host="$SUPABASE_DB_HOST" --port="$DDL_PORT"
      --username="$SUPABASE_DB_USER" --dbname="$SUPABASE_DB_NAME"
      --no-password --quiet --set ON_ERROR_STOP=1)

echo "==> Connectivity check"
"${PSQL[@]}" -c "select current_database() as database, current_user as user, version();"

echo "==> Applying ${SCHEMA_FILE}"
"${PSQL[@]}" -f "$SCHEMA_FILE"

echo "==> Tables now present"
"${PSQL[@]}" -c "\dt public.*"

echo "==> Row counts (must all be 0)"
"${PSQL[@]}" -c "select 'customers' as table, count(*) from customers
                 union all select 'products', count(*) from products
                 union all select 'orders', count(*) from orders
                 union all select 'order_items', count(*) from order_items;"

echo "==> Done. Schema created, no data inserted."

