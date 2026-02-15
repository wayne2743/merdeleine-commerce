#!/usr/bin/env bash
set -euo pipefail

echo "Running per-database init scripts..."

run_sql () {
  local db="$1"
  local file="$2"
  echo "==> Initializing DB: ${db} with ${file}"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$db" -f "$file"
}

run_sql "catalog_db"              "/docker-entrypoint-initdb.d/sql/catalog_service.sql"
run_sql "threshold_db"            "/docker-entrypoint-initdb.d/sql/threshold_service.sql"
run_sql "order_db"                "/docker-entrypoint-initdb.d/sql/order_service.sql"
run_sql "payment_db"              "/docker-entrypoint-initdb.d/sql/payment_service.sql"
run_sql "notification_db"         "/docker-entrypoint-initdb.d/sql/notification_service.sql"
run_sql "production_db"           "/docker-entrypoint-initdb.d/sql/production_service.sql"
run_sql "production_planning_db"  "/docker-entrypoint-initdb.d/sql/production_planning_service.sql"

echo "Per-database init done."
