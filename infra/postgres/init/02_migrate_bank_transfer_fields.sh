#!/bin/bash

# Payment Service Database Migration Script
# Description: Upgrade existing payment databases with new fields for bank transfer info
# Usage: ./02_migrate_bank_transfer_fields.sh <db_host> <db_port> <db_name> <db_user>

set -e

# Default values
DB_HOST="${1:-localhost}"
DB_PORT="${2:-5432}"
DB_NAME="${3:-payment_db}"
DB_USER="${4:-merdeleine}"

echo "=========================================="
echo "Payment Service Migration: Bank Transfer Fields"
echo "=========================================="
echo "Database: $DB_HOST:$DB_PORT/$DB_NAME"
echo "User: $DB_USER"
echo ""

# Execute migration
echo "Executing migration..."
psql -h "$DB_HOST" -p "$DB_PORT" -d "$DB_NAME" -U "$DB_USER" << EOF

-- Add bank_last_five column
ALTER TABLE payment
ADD COLUMN IF NOT EXISTS bank_last_five VARCHAR(5);

COMMENT ON COLUMN payment.bank_last_five IS '銀行帳號後五碼 (ECPay ATM payment)';

-- Add transfer_at column
ALTER TABLE payment
ADD COLUMN IF NOT EXISTS transfer_at TIMESTAMPTZ;

COMMENT ON COLUMN payment.transfer_at IS '匯款日期時間 (ECPay ATM payment callback timestamp)';

-- Verify columns were added
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'payment'
  AND column_name IN ('bank_last_five', 'transfer_at')
ORDER BY ordinal_position;

EOF

echo ""
echo "=========================================="
echo "Migration completed successfully!"
echo "=========================================="

