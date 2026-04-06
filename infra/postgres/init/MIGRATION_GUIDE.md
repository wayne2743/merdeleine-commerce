# Payment Database Migration Guide

## Overview
This guide provides instructions for upgrading an existing payment database to include the new bank transfer fields.

## New Fields Added
- **bank_last_five** (VARCHAR(5)): 銀行帳號後五碼 (Bank account last 5 digits from ECPay ATM transfers)
- **transfer_at** (TIMESTAMPTZ): 匯款日期時間 (Transfer timestamp from ECPay callback)

## Pre-Migration Checklist
- [ ] Backup the existing database
- [ ] Verify the database is accessible and running
- [ ] Ensure you have necessary database user credentials
- [ ] Stop or pause the payment-service if needed

## Option 1: Manual SQL Execution

### Using psql CLI
```bash
# Connect to your database
psql -h <db_host> -p <db_port> -d payment_db -U merdeleine

# Run the migration script
\i 02_add_bank_transfer_fields.sql
```

### Inline SQL
```sql
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
```

## Option 2: Bash Script Execution (Linux/Mac)

```bash
# Make the script executable
chmod +x ./02_migrate_bank_transfer_fields.sh

# Run migration with default settings (localhost:5432)
./02_migrate_bank_transfer_fields.sh

# Or with custom database parameters
./02_migrate_bank_transfer_fields.sh 100.113.120.124 5432 payment_db merdeleine
```

## Option 3: Docker Container

If running PostgreSQL in Docker:

```bash
# Copy migration script to container
docker cp ./02_add_bank_transfer_fields.sql <container_id>:/tmp/

# Execute migration inside container
docker exec <container_id> psql -U merdeleine -d payment_db -f /tmp/02_add_bank_transfer_fields.sql
```

## Option 4: Via docker-compose

```bash
# If using docker-compose.yml
docker-compose exec postgres psql -U merdeleine -d payment_db -f /docker-entrypoint-initdb.d/sql/02_add_bank_transfer_fields.sql
```

## Verification

After migration, verify the columns were added successfully:

```sql
-- Check that columns exist and have correct types
SELECT 
    column_name, 
    data_type, 
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'payment' 
  AND column_name IN ('bank_last_five', 'transfer_at')
ORDER BY ordinal_position;

-- Expected output:
--  column_name   | data_type | is_nullable | column_default
-- ---------------+-----------+-------------+----------------
--  bank_last_five | character varying | YES |
--  transfer_at    | timestamp with time zone | YES |

-- Check table structure
\d payment
```

## Rollback Plan (if needed)

If you need to rollback the migration:

```sql
-- Remove the newly added columns
ALTER TABLE payment
DROP COLUMN IF EXISTS bank_last_five;

ALTER TABLE payment
DROP COLUMN IF EXISTS transfer_at;
```

## Post-Migration Steps
1. Verify the payment-service application starts without errors
2. Test ECPay payment callbacks to ensure new fields are being populated
3. Query the payment table to verify data is being stored correctly
4. Monitor logs for any issues

## Troubleshooting

### Column Already Exists
If you get an error like "column 'bank_last_five' already exists":
- The column may have been added in a previous attempt
- Check the table structure with `\d payment`
- Verify both columns exist before running again

### Permission Denied
If you get permission errors:
- Ensure the database user has ALTER TABLE privileges
- Check the database user's role permissions
- Contact your database administrator

### Connection Failed
If you cannot connect to the database:
- Verify the database host, port, and credentials
- Check that PostgreSQL is running
- Verify network connectivity to the database server
- Check firewall rules if accessing a remote database

## Files Included
- `02_add_bank_transfer_fields.sql` - Main migration SQL script
- `02_migrate_bank_transfer_fields.sh` - Bash script wrapper for easy execution
- `MIGRATION_GUIDE.md` - This guide

## References
- Payment Entity: `services/payment-service/src/main/java/com/merdeleine/payment/entity/Payment.java`
- Database Schema: `docs/sql/sql/payment_service.sql`
- Infrastructure Init: `infra/postgres/init/sql/payment_service.sql`

